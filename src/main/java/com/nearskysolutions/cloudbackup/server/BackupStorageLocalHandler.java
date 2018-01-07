package com.nearskysolutions.cloudbackup.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.catalina.TrackedWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import com.nearskysolutions.cloudbackup.common.BackupFileAttributes;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.BackupRestoreRequestService;
import com.nearskysolutions.cloudbackup.util.FileZipUtils;

@Component(value="LocalBackupStorage")
public class BackupStorageLocalHandler implements BackupStorageHandler {
	
	Logger logger = LoggerFactory.getLogger(BackupStorageLocalHandler.class);
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	@Autowired
	private BackupRestoreRequestService restoreSvc;
	
	@Value( "${com.nearskysolutions.cloudbackup.localstore.fileStorageRootDir}" )
	private String fileStorageRootDir;
	
	@Value( "${com.nearskysolutions.cloudbackup.localstore.fileStorageRestoreDir}" )
	private String fileStorageRestoreDir;
	
	@Value( "${com.nearskysolutions.cloudbackup.localstore.maxRestoreSize}" )
	private int maxRestoreSize;
	
	private File systemTempDir;
	
	public BackupStorageLocalHandler() throws Exception {
		String sysProp = System.getProperty("java.io.tmpdir");
		
		if( null == sysProp ) {
			throw new Exception("Missing system property for java.io.tmpdir");
		}
		
		systemTempDir = new File(sysProp);
		
		if(false == systemTempDir.exists()) {
			throw new Exception(String.format("System temp directory '%s' doesn't exist.", sysProp));
		}
	}
	
	@Override 
	public void processBackupPacket(BackupFileDataPacket packet) throws Exception {
		
		if( null == packet ) {
			throw new Exception("packet can't be null");
		}
		
		logger.trace(String.format("In BackupStorageLocalHandler.processBackupPacket(BackupFileDataPacket packet): packetID = %s", packet.getDataPacketID()));
		
		UUID packetID = packet.getDataPacketID();
		UUID trackerID = packet.getFileTrackerID();
				
		BackupFileTracker tracker = dataSvc.getTrackerByBackupFileTrackerID(trackerID);
		
		UUID clientID = tracker.getClientID();
		File fileDir = getTrackerDirectory(tracker);
		
		File tmpFile = new File(String.format("%s%s%s",
								fileDir.getAbsolutePath(),
								File.separator,
								trackerID.toString()));
		
		File finalZipFile = new File(String.format("%s.zip", tmpFile.getAbsolutePath()));
		
		boolean retryTracker = false;
		
		try {				
			
			if( FileAction.Delete == packet.getFileAction() ) {
				
				logger.info(String.format("Tracker: %s for client: %s, marking as deleted", 
											trackerID.toString(), clientID.toString()));
				
				if(false == tracker.isDirectory() && finalZipFile.exists()) {	
					
					logger.info(String.format("Deleting stored file for tracker: %s for client: %s", 
												trackerID.toString(), clientID.toString()));
					
					//Delete file associated with tracker
					finalZipFile.delete();
				}	
				
				tracker.setTrackerStatus(BackupFileTrackerStatus.Deleted);
				
			} else if( tracker.isDirectory() ) { //Only files are physically stored
				
				logger.info(String.format("Tracker: %s represents directory: %s%s%s for client: %s, marking as stored", 
											trackerID.toString(), tracker.getSourceDirectory(), File.separator, tracker.getFileName(), clientID.toString()));
				
				tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);
								
			} else {

				logger.info(String.format(" Processing update for tracker: %s representing file: %s%s%s for client: %s, marking as stored", 
											trackerID.toString(), tracker.getSourceDirectory(), File.separator, tracker.getFileName(), clientID.toString()));
								
				if( 1 != packet.getPacketNumber() ) {
				
					if(BackupFileTrackerStatus.Processing != tracker.getTrackerStatus() ) {						
						throw new Exception(String.format("Invalid state for packet number out of order and can't process for packetID: %s, tracker ID: %s", 
															packetID.toString(), trackerID.toString()));						
					} else	if(false == tmpFile.exists() ) {						
						retryTracker = true;
					
						throw new Exception(String.format("Packet number out of order and can't process for packetID: %s, tracker ID: %s", packetID.toString(), trackerID.toString()));
					}
					
				} else {				
				
					tracker.setTrackerStatus(BackupFileTrackerStatus.Processing);
				
					if(fileDir.exists())  {					
					
						logger.info(String.format("Starting new stored file for tracker ID: %s, client ID: %s", trackerID.toString(), clientID.toString()));
						
						//Replace final file if it exists and this is the first new packet
						if(finalZipFile.exists()) {
							finalZipFile.delete();
						}
						
					} else {
						
						logger.info(String.format("Createing file directory %s for tracker ID: %s", fileDir.getAbsolutePath(), clientID.toString()));
						
						//Ceate tracker dir with all parents when needed			
						fileDir.mkdirs();
					}					
				}
				
				logger.info(String.format("Writing packet data for packet %s, packet number: %d of %d", 
								packetID.toString(), packet.getPacketNumber(), packet.getPacketsTotal()));
				
				ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(packet.getFileData()));
									
				try (FileOutputStream fos = new FileOutputStream(tmpFile, true)) {										
					FileZipUtils.WriteZipInputToOutput(bais, fos);														
				}
				
				if( packet.getPacketsTotal() != packet.getPacketNumber() ) {	
					logger.info(String.format("Completed write, leaving temp file for packet %s, last packet number: %d of %d", 
							packetID.toString(), packet.getPacketNumber(), packet.getPacketsTotal()));
				} else {
								
					logger.info(String.format("Completed write for last packet of tracker: %s, comparing checksum digest", trackerID.toString())); 
					
					MessageDigest messageDigest = MessageDigest.getInstance("MD5");
					 
					try(FileInputStream fis =  new FileInputStream(tmpFile)) {
						byte[] mdBytes = new byte[4096];
				       
				        int numRead;

				        do {
				           numRead = fis.read(mdBytes);
				           if (numRead > 0) {
				        	   messageDigest.update(mdBytes, 0, numRead);
				           }
				        } while (numRead != -1);	      
					}
					
					byte[] md5Complete = messageDigest.digest();
					String md5Encoded = Base64.getEncoder().encodeToString(md5Complete);

					if( false == md5Encoded.equals(tracker.getLastDigest()) ) {						
						logger.info(String.format("Checksum mis-match for tracker: %s, marking tracker for retry", trackerID.toString()));
						
						retryTracker = true;
						
						throw new Exception("Unable to file due to checksum error");
					} else {						
						logger.info(String.format("Checksum match for tracker: %s, creating final storage file", trackerID.toString()));
						
						FileZipUtils.CreateZipFileOutput(tmpFile, finalZipFile);
						tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);
					}						
				}					
				
			}				
						
		} catch (Exception ex) {		
						
			logger.error("Unable to process data packets due to exception", ex);
			
			if( null != tracker ) {
				
				tracker.setLastError(ex.getMessage());
				
				if( retryTracker ) {
					tracker.setTrackerStatus(BackupFileTrackerStatus.Retry);
				} else {
					tracker.setTrackerStatus(BackupFileTrackerStatus.Error);
				}
								
			}			
			
		} finally {
			
			//Remove temp file if in any other state other than continuing to process
			if( BackupFileTrackerStatus.Processing != tracker.getTrackerStatus() && true == tmpFile.exists() ) {				
				logger.info(String.format("Deleting temporary storage file tracker: %s", trackerID.toString()));
				
				tmpFile.delete();
			}
			
		}
		
		
		logger.info(String.format("Setting status and updating state for tracker: %s, status: %s", trackerID.toString(), tracker.getTrackerStatus().toString()));
		
		tracker.setLastStatusChange(new Date());
			
		this.dataSvc.updateBackupFileTracker(tracker);
		
		logger.trace(String.format("Completed BackupStorageLocalHandler.processBackupPacket(BackupFileDataPacket packet): packetID = %s", packet.getDataPacketID()));
	}

	 	 
	private File getTrackerDirectory(BackupFileTracker tracker) {
		
		File fileDir = new File(String.format("%s%s%s%s%s", 
								this.fileStorageRootDir,
								File.separator,
								tracker.getClientID().toString(),
								File.separator,
								tracker.getBackupFileTrackerID().toString().substring(0, 2)));
		return fileDir;
	}
	
	@Override
	public void recreateTrackerFiles(BackupRestoreRequest restoreRequest) {
		
		logger.trace("In BackupStorageLocalHandler.recreateTrackerFiles(BackupRestoreRequest restoreRequest)");
		
		File restoreTempDir = new File(String.format("%s%s%s", this.systemTempDir, File.separator, UUID.randomUUID().toString()));
		
		logger.info(String.format("Restoring files to temp dir: %s", restoreTempDir));
		
		try {			
			
			if( null == restoreRequest ) {
				throw new NullPointerException("Restore request parameter can't be null");
			}
			
			int totalFileSize = 0;
			List<UUID> trackerIDList = restoreRequest.getRequestedFileTrackerIDs();			
						
			if( null == trackerIDList || 0 == trackerIDList.size() ) {
				throw new NullPointerException("Tracker list can't be null or empty");
			} 
			
			restoreRequest.setCurrentStatus(RestoreStatus.Initializing);
						
			restoreSvc.updateRestoreRequest(restoreRequest);
						
			List<BackupFileTracker> trackerList = new ArrayList<BackupFileTracker>();
			
			for(UUID trackerID : trackerIDList) {
				BackupFileTracker bft = this.dataSvc.getTrackerByBackupFileTrackerID(trackerID);
				
				if( null == bft ) {
					throw new Exception(String.format("No file tracker found for id: %d", trackerID));
				} else if (!bft.getClientID().equals(restoreRequest.getClientID())) {
					throw new Exception(String.format("Client ID mismatch for file tracker: %d, found %s, expected %s", 
										trackerID, bft.getClientID(), restoreRequest.getClientID()));
				}
			
				logger.info(String.format("Adding requested tracker id: %s to restore reuqest", bft.getBackupFileTrackerID()));
				
				trackerList.add(bft);
			}
			
			if( true == restoreRequest.isIncludeSubdirectories() ) {
				List<BackupFileTracker> newTrackerList = new ArrayList<BackupFileTracker>(); 
							
				List<BackupFileTracker> clientTrackerList = dataSvc.getAllBackupTrackersForClient(restoreRequest.getClientID());
				
				//Recursively scan and add any child directories and files
				for(BackupFileTracker tracker : trackerList) {
					createFileTreeForTracker(tracker, newTrackerList, clientTrackerList);
				}
				
				trackerList = newTrackerList;
			}
			
			logger.info(String.format("Starting processing of %d tracker instances for restore request with id: %s", 
							trackerList.size(), restoreRequest.getRequestID().toString()));
			
			restoreRequest.setCurrentStatus(RestoreStatus.Processsing);
			restoreRequest.setProcessingStartDateTime(new Date());
			
			restoreSvc.updateRestoreRequest(restoreRequest);
											
			for(BackupFileTracker tracker : trackerList) {
				
				logger.info("Restoring tracker %d, file name: %s, from request: %s", 
							tracker.getBackupFileTrackerID(), tracker.getFileName(), restoreRequest.getRequestID().toString());
				
				//Check for external change of request status
				BackupRestoreRequest latestRestoreRequest = restoreSvc.getRestoreRequestByRequestID(restoreRequest.getRequestID());
				
				if( null != latestRestoreRequest && RestoreStatus.Processsing == latestRestoreRequest.getCurrentStatus() ) {
					
					totalFileSize += restoreFileFromTracker(restoreRequest.getClientID(), 
															restoreTempDir, 
															tracker);
				
					logger.info(String.format("Current restore size for restore request: %s = %d", 
									restoreRequest.getRequestID().toString(), totalFileSize));
					
					if( totalFileSize > this.maxRestoreSize ) {
						throw new Exception("Maximum restore size reached, decrease requested restores");
					}
					
				} else { //State has changed since request started
													
					throw new Exception(String.format("Status has changed for request ID: %s since start of restore process, aborting", restoreRequest.getRequestID()));
				}
			}
			
			logger.info(String.format("Final restore size for restore request: %s = %d", 
										restoreRequest.getRequestID().toString(), totalFileSize));
			
			
			String finalRestoreFileName = String.format("%s%s%s.zip", this.fileStorageRestoreDir, File.separator, UUID.randomUUID().toString());
			File finalRestoreFile = new File(finalRestoreFileName);
			
			logger.info(String.format("Writing restored to final archive: %s for restore request: %s", 
						finalRestoreFileName, restoreRequest.getRequestID().toString()));
					
			//This is needed to keep empty directories in the zip archive
			processEmptyDirectoriesForZip(restoreTempDir);
			
			FileZipUtils.CreateZipFileOutput(restoreTempDir, restoreTempDir.getAbsolutePath(), finalRestoreFile);
				
			restoreRequest.setCurrentStatus(RestoreStatus.Success);			
			restoreRequest.setCompletedDateTime(new Date());
			restoreRequest.setRestoreResultsURLs(new ArrayList<String>());
			
			//TODO Change to URL format
			restoreRequest.getRestoreResultsURLs().add(finalRestoreFileName);
			
			restoreSvc.updateRestoreRequest(restoreRequest);
			
			logger.info(String.format("Restore complete for restore request: %s", restoreRequest.getRequestID().toString()));
			
		} catch (Exception ex) {			
			logger.error("Unable to recreate tracker files due to exception:", ex);
			
			try {
				restoreRequest.setCurrentStatus(RestoreStatus.Error);
				restoreRequest.setErrorMessage(ex.getLocalizedMessage());
				restoreRequest.setCompletedDateTime(new Date());
				
				restoreSvc.updateRestoreRequest(restoreRequest);				
			} catch (Exception subEx) {
				logger.error("Unable to update restore request to error status due to execption:", ex);
			}			
		} finally {					

			if( restoreTempDir.exists() ) {				
				 FileSystemUtils.deleteRecursively(restoreTempDir);
			}
		}
		
		logger.trace("Completed BackupStorageLocalHandler.recreateTrackerFiles(BackupRestoreRequest restoreRequest)");
	}

	private void processEmptyDirectoriesForZip(File currentDir) throws IOException{

		File[] childFiles = currentDir.listFiles(); 
		if( childFiles.length == 0 ) {
						
			File f = new File(String.format("%s%s.empty", currentDir.getAbsolutePath(), File.separator));
			f.createNewFile();
			
		} else {
			for(int i = 0; i < childFiles.length; i++) {
				if( childFiles[i].isDirectory() ) {
					processEmptyDirectoriesForZip(childFiles[i]);
				}
			}
		}		
	}

	private int restoreFileFromTracker(UUID clientID, 
									   File restoreRootDir, 
									   BackupFileTracker tracker) throws Exception {
				
		File trackerDir = getTrackerDirectory(tracker);
		
		int totalFileSize = 0;
		
		if(false == tracker.isDirectory() && false == trackerDir.exists()) {
			throw new Exception(String.format("Couldn't find directory for tracker ID: %s - %s",
								tracker.getBackupFileTrackerID(), trackerDir));
		}		
		
		String restorePath;
		//TODO Make configurable
		int driveLetterIdx = tracker.getSourceDirectory().indexOf(":");
		
		if( 0 > driveLetterIdx ) {
			restorePath = tracker.getSourceDirectory();
		} else {
			restorePath = tracker.getSourceDirectory().substring(driveLetterIdx + 1);
		}
									
		File restoreFile = new File(String.format("%s%s%s%s%s", 
													restoreRootDir.getAbsolutePath(), 
													File.separator, 
													restorePath, 
													File.separator, 
													tracker.getFileName()));
		
		File[] dirFiles = trackerDir.listFiles(new FilenameFilter() {					
			@Override
			public boolean accept(File dir, String name) {						
				return name.toLowerCase().endsWith(".zip");
			}
		});
		
		
		if(false == tracker.isDirectory() && 0 == dirFiles.length) {
			throw new Exception(String.format("No files found in storage directory: %s", trackerDir));
		}
		
		if(false == restoreFile.getParentFile().exists()) {
			logger.info(String.format("Creating restore directory: %s", restoreFile.getParentFile().getAbsolutePath()));
			
			restoreFile.getParentFile().mkdirs();
		}
		
		//TODO Test using directory tracker
		if( tracker.isDirectory() ) {
			if( false == restoreFile.exists() ) {
				restoreFile.mkdir();
			}						
		} else {
			
			FileOutputStream fos = null;			
			boolean processComplete = false;
			int retryCount = 0;
			int maxAttempts = 10;
			boolean fileFound;
			
			while( false == processComplete && maxAttempts > retryCount++ ) {
				try {
					
					fos = new FileOutputStream(restoreFile);
					
					fileFound = false;
					
					for(int i = 0; i < dirFiles.length && false == fileFound; i++) {
												
						if(dirFiles[i].getName().toLowerCase().endsWith(String.format("%s.zip", tracker.getBackupFileTrackerID().toString())) ) {
							fileFound = true;
								
							logger.info(String.format("Saving restore file for tracker ID: %s", tracker.getBackupFileTrackerID().toString()));
								
							FileZipUtils.WriteZipFileToOutput(dirFiles[i], fos);						
						}
					}
					
					if( fileFound == false ) { 
						throw new Exception(String.format("Missing file for tracker ID: %s", tracker.getBackupFileTrackerID().toString()));
					}
					
					processComplete = true;
				} catch(FileNotFoundException ex) {
					
					//Backoff when access is denied, may be temporary
					if( 0 < ex.getMessage().indexOf("Access is denied") && maxAttempts > retryCount	) {						
						logger.info(String.format("Access denied detected on attempt %d of temp restore file write, backing off", retryCount));
						Thread.sleep(500);
						
						if(restoreFile.exists()) {
							restoreFile.delete();
						}
					} else {
						//Throw any other exception
						throw ex;
					}
				
				} finally {
					if( null != fos ) {
						fos.close();
					}
				}
			}
			
			totalFileSize += dirFiles[0].length();		
			
		}
		
		setFileAttributes(restoreFile, tracker.getFileAttributes());
		
		return totalFileSize;
	}

	private void createFileTreeForTracker(BackupFileTracker tracker, List<BackupFileTracker> newTrackerList, List<BackupFileTracker> clientTrackerList) {
		
		if( tracker.isDirectory() ) {
			for(BackupFileTracker clientTracker : clientTrackerList) {
				//Since tracker is a directory, find all other trackers which
				//have its name as a parent
				if( clientTracker.getSourceDirectory() != null && 
					clientTracker.getSourceDirectory().toLowerCase().equals(tracker.getFileReference().getAbsolutePath().toLowerCase())) {

					createFileTreeForTracker(clientTracker, newTrackerList, clientTrackerList);
					
					logger.info(String.format("Adding child tracker id: %s to restore request", clientTracker.getBackupFileTrackerID()));
					
					newTrackerList.add(clientTracker);
				}
			}
		}
		
		newTrackerList.add(tracker);
		
	}
	
	
	private void setFileAttributes(File restoreFile, BackupFileAttributes fileAttributes) throws IOException {
	    
	    //TODO Make configurable

		//Note: Not all saved attributes are used because some
		// 		are derived
	
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
	    				   "basic:lastModifiedTime", 
	    				   FileTime.fromMillis(fileAttributes.getFileModifiedDateTimeMillis()),
	    				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "basic:creationTime", 
				   FileTime.fromMillis(fileAttributes.getFileCreatedDateTimeMillis()),
				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "basic:lastAccessTime", 
				   FileTime.fromMillis(fileAttributes.getFileAccessDateTimeMillis()),
				   LinkOption.NOFOLLOW_LINKS);	    
	 	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "dos:archive", 
				   fileAttributes.isArchive(),
				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "dos:hidden", 
				   fileAttributes.isHidden(),
				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "dos:readonly", 
				   fileAttributes.isReadOnly(),
				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "dos:system", 
				   fileAttributes.isSystem(),
				   LinkOption.NOFOLLOW_LINKS);
		
	}
}
