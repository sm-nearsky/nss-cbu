package com.nearskysolutions.cloudbackup.prod.beans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import javax.persistence.Column;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileAttributes;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.FileHandlerService;

@Component(value="LocalBackupStorage")
public class BackupStorageLocalHandler implements BackupStorageHandler {
	
	Logger logger = LoggerFactory.getLogger(BackupStorageLocalHandler.class);
	
	@Autowired	
	private FileHandlerService fileHandlerSvc;
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	@Value( "${com.nearskysolutions.cloudbackup.localstore.fileStorageRootDir}" )
	private String fileStorageRootDir;
	
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
					
					createStorageFile(baos, trackerID, fileDir);
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

	private void createStorageFile(ByteArrayOutputStream baos, Long trackerID, File fileDir)
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
		
		File storageFile = new File(String.format("%s%s%s.gz",
													fileDir.getAbsolutePath(),
													File.separator,
													UUID.randomUUID()));		
		
		FileOutputStream fos = null;
		byte[] fileBytes = baos.toByteArray();
		int bufferSize = 4096;
		int bytesRead = 0;
		int readSize;
		
		try {
			fos = new FileOutputStream(storageFile);
		
			while(bytesRead < baos.toByteArray().length) {
				readSize = Math.min(bufferSize, fileBytes.length - bytesRead);
				
				fos.write(fileBytes, bytesRead, readSize);
				
				bytesRead += readSize;
			}
			
			fos.flush();
			fos.close();
			
		} finally {
			if( null != fos ) {
				fos.close();
			}
		}
	}
	
	@Override
	public void recreateTrackerFiles(UUID clientID, 
									 List<Long> trackerIDList, 
									 String restoreTarget, 
									 boolean isIncludeSubdirectories) throws Exception {
		try {
			
//			String restoreRootDir = "/C:/tmp/nssCbuFileRestore";
//			
//			UUID clientID = UUID.fromString("57649898-ec95-48ab-a257-4bf7cbb971c9");
			
			if( null == clientID ) {
				throw new NullPointerException("Client ID parameter can't be null");
			} else if( null == trackerIDList || 0 == trackerIDList.size() ) {
				throw new NullPointerException("Tracker list can't be null or empty");
			} else if( null == restoreTarget ) {
				throw new NullPointerException("Restore target can't be null");
			}
			
			String restoreRootDir = restoreTarget;
			
			List<BackupFileTracker> trackerList = new ArrayList<BackupFileTracker>();
			
			for(Long trackerID : trackerIDList) {
				BackupFileTracker bft = this.dataSvc.getTrackerByBackupFileTrackerID(trackerID);
				
				if( null == bft ) {
					throw new Exception(String.format("No file tracker found for id: %d", trackerID));
				} else if (!bft.getClientID().equals(clientID)) {
					throw new Exception(String.format("Client ID mismatch for file tracker: %d, found %s, expected %s", 
										trackerID, bft.getClientID(), clientID));
				}
				
				trackerList.add(bft);
			}
			
			if( true == isIncludeSubdirectories ) {
				List<BackupFileTracker> newTrackerList = new ArrayList<BackupFileTracker>(); 
							
				List<BackupFileTracker> clientTrackerList = dataSvc.getAllBackupTrackersForClient(clientID);
				
				//Recursively scan and add any child directories and files
				for(BackupFileTracker tracker : trackerList) {
					createFileTreeForTracker(tracker, newTrackerList, clientTrackerList);
				}
				
				trackerList = newTrackerList;
			}
			
			for(BackupFileTracker tracker : trackerList) {
				File trackerDir = getFileTrackerDirectoryForClient(clientID.toString(), tracker.getBackupFileTrackerID()); 
				
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
												restoreRootDir, 
												File.separator, 
												restorePath, 
												File.separator, 
												tracker.getFileName()));
				
				File[] dirFiles = trackerDir.listFiles(new FilenameFilter() {					
					@Override
					public boolean accept(File dir, String name) {						
						return name.toLowerCase().endsWith(".gz");
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
				
				//TODO Handle sub directories
				if( tracker.isDirectory() ) {
					if( false == restoreFile.exists() ) {
						restoreFile.mkdir();		
						setFileAttributes(restoreFile, tracker.getFileAttributes());
					}
				} else {
					createFileFromGZip(dirFiles[0], restoreFile);
					
					setFileAttributes(restoreFile, tracker.getFileAttributes());
				}				
			}
			
		} catch (Exception ex) {			
			logger.error("Unable to recreate tracker files due to exception:", ex);
		}
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

	private void createFileFromGZip(File sourceFile, File destFile) throws IOException {
		
		GZIPInputStream gis = null;
		FileOutputStream gzipOut = null;
		int byteCount;
		byte[] readBytes = new byte[1024];
		
		logger.info(String.format("Restoring compressed data to final file: %s", destFile.getAbsolutePath()));
		
		try
		{
			gis = new GZIPInputStream(new FileInputStream(sourceFile));
		
		    gzipOut = new FileOutputStream(destFile);
		    
		    while ((byteCount = gis.read(readBytes)) > 0) {
		    	gzipOut.write(readBytes, 0, byteCount);
		    }	    			        
		 
		} catch (IOException ex) {		
			throw ex;		
		} finally {			
			
			if(null != gis) {
				gis.close();
			}
			
			if(null != gzipOut) {
				gzipOut.close();
			}		
		}
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
