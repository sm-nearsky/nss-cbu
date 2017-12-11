package com.nearskysolutions.cloudbackup.prod.beans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import com.nearskysolutions.cloudbackup.common.BackupFileAttributes;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.BackupRestoreRequestService;
import com.nearskysolutions.cloudbackup.services.FileHandlerService;
import com.nearskysolutions.cloudbackup.util.FileZipUtils;

@Component(value="LocalBackupStorage")
public class BackupStorageLocalHandler implements BackupStorageHandler {
	
	Logger logger = LoggerFactory.getLogger(BackupStorageLocalHandler.class);
	
	@Autowired	
	private FileHandlerService fileHandlerSvc;
	
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
	public void retrieveAndProcessBackupPackets(Long batchID) {
										
		try {		
			BackupFileDataBatch fileBatch = dataSvc.getDataBatchByBatchID(batchID);
			String clientDir = fileBatch.getClientID().toString();
			List<BackupFileDataPacket> packetList = this.fileHandlerSvc.retrieveBatchFromProcessingQueue(fileBatch.getFileBatchID());
			
			while(packetList.size() > 0) {				
				Long trackerID = packetList.get(packetList.size()-1).getFileTrackerID();
				BackupFileTracker tracker = dataSvc.getTrackerByBackupFileTrackerID(trackerID);
				List<BackupFileDataPacket> packetsForFile = new ArrayList<BackupFileDataPacket>();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				for(int i = packetList.size(); --i >= 0;) {					
					if( packetList.get(i).getFileTrackerID().longValue() == trackerID.longValue()) {
						packetsForFile.add(packetList.get(i));
						packetList.remove(i);
					}					
				}	
				
				File fileDir = getFileTrackerDirectoryForClient(clientDir, trackerID);
				
				if( 1 == packetsForFile.size() && FileAction.Delete == packetsForFile.get(0).getFileAction() ) {
				
					if(fileDir.exists()) {					
						//Clear any files
						for(File file : fileDir.listFiles()) {
							file.delete();
						}
						
						//Delete tracker directory
						fileDir.delete();
					}
					
				} else {
					packetsForFile.sort(new Comparator<BackupFileDataPacket>() {			    
						@Override
						public int compare(BackupFileDataPacket p1, BackupFileDataPacket p2) {			
							return Integer.compare(p1.getPacketNumber(), p2.getPacketNumber());
						} });
									
					if( false == tracker.isDirectory() ) {
						for(BackupFileDataPacket packet : packetsForFile) {
							baos.write(Base64.getDecoder().decode(packet.getFileData()));					
						}
					}
					
					createStorageFile(baos.toByteArray(), String.format("%s.zip", UUID.randomUUID().toString()), fileDir);
					
					tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);
					
					this.dataSvc.updateBackupFileTracker(tracker);
				}						
			}		
			
		} catch (Exception ex) {		
						
			logger.error("Unable to process data packets due to exception", ex);
			
			try {
				this.dataSvc.setBatchError(batchID, ex.getMessage());
			} catch(Exception ex2) {
				logger.error("Could not set batch error due to exception on save", ex2);
			}
		}
	}

	private File getFileTrackerDirectoryForClient(String clientDir, Long trackerID) {
		File fileDir = new File(String.format("%s%s%s%s%s", 
				this.fileStorageRootDir,
				File.separator,
				clientDir,
				File.separator,
				trackerID));
		return fileDir;
	}

	private void createStorageFile(byte[] fileBytes, String sourceFileName, File fileDir)
			throws FileNotFoundException, IOException {					

		if(fileDir.exists()) {
			
			//Replace file if tracker dir exists
			for(File file : fileDir.listFiles()) {
				file.delete();
			}
			
		} else {
			
			//Otherwise create tracker dir with all parents			
			fileDir.mkdirs();
		}
		
		File storageFile = new File(String.format("%s%s%s",
									fileDir.getAbsolutePath(),
									File.separator,
									sourceFileName));
		
		FileOutputStream fos = null;		
		int bufferSize = 4096;
		int bytesRead = 0;
		int readSize;
		
		try {
			fos = new FileOutputStream(storageFile);
		
			while(bytesRead < fileBytes.length) {
				readSize = Math.min(bufferSize, fileBytes.length - bytesRead);
				
				fos.write(fileBytes, bytesRead, readSize);
				
				bytesRead += readSize;
			}
			
			fos.flush();
			fos.close();
			
//			GZipUtils.CreateZipFileOutput(storageFile, new File(String.format("%s%s%s.zip",
//																fileDir.getAbsolutePath(),
//																File.separator,
//																UUID.randomUUID())));
			
		} finally {
			if( null != fos ) {
				fos.close();
			}
		}
	}
	
	@Override
	public void recreateTrackerFiles(BackupRestoreRequest restoreRequest) {
		
		File restoreTempDir = new File(String.format("%s%s%s", this.systemTempDir, File.separator, UUID.randomUUID().toString()));
		int totalFileSize = 0;
		
		try {			
			
			if( null == restoreRequest ) {
				throw new NullPointerException("Restore request parameter can't be null");
			} 
						
			List<Long> trackerIDList = restoreRequest.getRequestedFileTrackerIDs();			
						
			if( null == trackerIDList || 0 == trackerIDList.size() ) {
				throw new NullPointerException("Tracker list can't be null or empty");
			} 
			
			restoreRequest.setCurrentStatus(RestoreStatus.Initializing);
						
			restoreSvc.updateRestoreRequest(restoreRequest);
			
			
			List<BackupFileTracker> trackerList = new ArrayList<BackupFileTracker>();
			
			for(Long trackerID : trackerIDList) {
				BackupFileTracker bft = this.dataSvc.getTrackerByBackupFileTrackerID(trackerID);
				
				if( null == bft ) {
					throw new Exception(String.format("No file tracker found for id: %d", trackerID));
				} else if (!bft.getClientID().equals(restoreRequest.getClientID())) {
					throw new Exception(String.format("Client ID mismatch for file tracker: %d, found %s, expected %s", 
										trackerID, bft.getClientID(), restoreRequest.getClientID()));
				}
				
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
			
			restoreRequest.setCurrentStatus(RestoreStatus.Processsing);
			restoreRequest.setProcessingStartDateTime(new Date());
			
			restoreSvc.updateRestoreRequest(restoreRequest);
												

			
			for(BackupFileTracker tracker : trackerList) {
				
				//Check for external change of request status
				BackupRestoreRequest latestRestoreRequest = restoreSvc.getRestoreRequestByRequestID(restoreRequest.getRequestID());
				
				if( null != latestRestoreRequest && RestoreStatus.Processsing == latestRestoreRequest.getCurrentStatus() ) {
					
					totalFileSize += restoreFileFromTracker(restoreRequest.getClientID(), 
															restoreTempDir, 
															tracker);
					
					if( totalFileSize > this.maxRestoreSize ) {
						throw new Exception("Maximum restore size reached, decrease requested restores");
					}
					
				} else { //State has changed since request started
													
					throw new Exception(String.format("Status has changed for request ID: %s since start of restore process, aborting", restoreRequest.getRequestID()));
				}
			}
			
			String finalRestoreFileName = String.format("%s%s%s.zip", this.fileStorageRestoreDir, File.separator, UUID.randomUUID().toString());
			File finalRestoreFile = new File(finalRestoreFileName);
			
			FileZipUtils.CreateZipFileOutput(restoreTempDir, restoreTempDir.getAbsolutePath(), finalRestoreFile);
				
			restoreRequest.setCurrentStatus(RestoreStatus.Success);			
			restoreRequest.setCompletedDateTime(new Date());
			restoreRequest.setRestoreResultsURLs(new ArrayList<String>());
			
			//TODO Change to URL format
			restoreRequest.getRestoreResultsURLs().add(finalRestoreFileName);
			
			restoreSvc.updateRestoreRequest(restoreRequest);
			
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
	}

	private int restoreFileFromTracker(UUID clientID, File restoreRootDir, BackupFileTracker tracker)
			throws Exception, IOException {
		File trackerDir = getFileTrackerDirectoryForClient(clientID.toString(), tracker.getBackupFileTrackerID()); 
		int totalFileSize = 0;
		
		if(false == trackerDir.exists()) {
			throw new Exception(String.format("Couldn't find directory for tracker ID: %d - %s",
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
		
		if(1 != dirFiles.length) {
			throw new Exception(String.format("Invalid number of files found in storage directory: %s, expected 1 found %d",
												trackerDir, dirFiles.length));
		}
		
		if(false == restoreFile.getParentFile().exists()) {
			logger.info(String.format("Creating restore directory: %s", restoreFile.getParentFile().getAbsolutePath()));
			
			restoreFile.getParentFile().mkdirs();
		}
		
		//TODO Test using directory tracker
		if( tracker.isDirectory() ) {
			if( false == restoreFile.exists() ) {
				restoreFile.mkdir();		
				setFileAttributes(restoreFile, tracker.getFileAttributes());
			}
		} else {
			FileZipUtils.CreateSingleFileFromZip(dirFiles[0], restoreFile);
			
			totalFileSize += dirFiles[0].length();
			
			setFileAttributes(restoreFile, tracker.getFileAttributes());
		}
		
		return totalFileSize;
	}

	private void createFileTreeForTracker(BackupFileTracker tracker, List<BackupFileTracker> newTrackerList, List<BackupFileTracker> clientTrackerList) {
		
		if( tracker.isDirectory() ) {
			for(BackupFileTracker clientTracker : clientTrackerList) {
				if( clientTracker.getSourceDirectory() != null && 
					clientTracker.getSourceDirectory().toLowerCase().equals(tracker.getFileReference().getAbsolutePath().toLowerCase())) {

					createFileTreeForTracker(clientTracker, newTrackerList, clientTrackerList);
					
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
