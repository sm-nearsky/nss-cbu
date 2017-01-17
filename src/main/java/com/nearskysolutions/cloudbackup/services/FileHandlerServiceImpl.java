package com.nearskysolutions.cloudbackup.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.FilePacketHandlerQueue;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.data.BackupFileDataBatchRepository;
import com.nearskysolutions.cloudbackup.data.BackupFileDataPacketRepository;
import com.nearskysolutions.cloudbackup.data.BackupFileTrackerRepository;

@Component 
public class FileHandlerServiceImpl implements FileHandlerService {

	Logger logger = LoggerFactory.getLogger(FileHandlerServiceImpl.class);
	
	@Autowired
	BackupFileTrackerRepository trackerRepo;
	
	@Autowired
	BackupFileDataBatchRepository batchRepo;
	
	@Autowired
	BackupFileDataPacketRepository packetRepo;
	
	@Autowired
	@Qualifier("PacketHandlerQueue")
	FilePacketHandlerQueue filePacketHandlerQueue;
		
	@Value( "${com.nearskysolutions.cloudbackup.client.packetStagingDir}" )
	private String localStagingDir;
	
	@Value( "${com.nearskysolutions.cloudbackup.general.filePacketSize}" )
	private int filePacketSize;
	
	@Override 
	public List<BackupFileTracker> updateFileTrackerListing(UUID clientID, 
																 String rootDir,
																 String repoType,
																 String repoLoc,
																 String repoKey) throws Exception {
				
		if( null == rootDir ) {
			logger.error("Null root directory argument passed to FileHandlerServiceImpl.updateFileTrackerListing");
			throw new NullPointerException("Root directory name can't be null");
		}
		
		List<BackupFileTracker> trackerFileList = new ArrayList<BackupFileTracker>();
						
		for(BackupFileTracker tracker : trackerRepo.findByClientID(clientID)) {
			if(tracker.getSourceDirectory().toUpperCase().startsWith(rootDir.toUpperCase())) {
				trackerFileList.add(tracker);
			}			
		}
		
		List<File> currentFileList = scanFilesForDirectory(rootDir);
		
		boolean fileFound;
		
		//Check for new or changed files
		for(File currentFile : currentFileList) {
			fileFound = false;
			
			for(BackupFileTracker tracker : trackerFileList) {		
		
				fileFound = currentFile.getAbsolutePath().equalsIgnoreCase(tracker.getFileReference().getAbsolutePath());
				
				if( fileFound ) {
					
					//Case where deleted file can be re-created before
					//tracker purge
					if(tracker.isFileDeleted()) {
												
						//Delete tracker and undo flag so 
						//a new one will be created
						trackerRepo.delete(tracker);
						fileFound = false;
					}
					break;
				}
			}
			
			if( false == fileFound ) {
				logger.info(String.format("Existing file not found, tracker added: %s", currentFile.getAbsolutePath()));
				
				BackupFileTracker newTracker = new BackupFileTracker(clientID, repoType, repoLoc, repoKey, currentFile.getAbsolutePath());
				
				newTracker.setFileChanged(true);
				newTracker.setFileNew(true);
				
				newTracker = this.trackerRepo.save(newTracker);
				
				trackerFileList.add(newTracker);
			}			
		}
		
		//Check for changed or deleted files
		for(BackupFileTracker tracker : trackerFileList) {		
			fileFound = false;
			
			for(File currentFile : currentFileList) {
				
				fileFound = currentFile.getAbsolutePath().equalsIgnoreCase(tracker.getFileReference().getAbsolutePath());
				
				if( fileFound ) {
					if( false == tracker.equalsFile(currentFile) ) {
						logger.info(String.format("Found changes for file: %s", tracker.getFileReference().getAbsolutePath()));
						
						tracker.updateFileAttributes(currentFile);						
						tracker.setFileChanged(true);
					}
				
					break;
				}
			}
			
			if( false == fileFound ) {
				logger.info(String.format("Tracker file not found and marked for deletion: %s", tracker.getFileReference().getAbsolutePath()));
				
				tracker.setFileChanged(true);
				tracker.setFileDeleted(true);
			}
			
			if(tracker.isFileChanged()) {
				trackerRepo.save(tracker);
			}			
		}
		
		return trackerFileList;
	}	
	
	private List<File> scanFilesForDirectory(String dir) throws IOException {
		List<File> files = new ArrayList<File>();
		
		if( null == dir ) {
			logger.error("Null directory argument passed to FileHandlerServiceImpl.scanFilesForDirectory");
			throw new NullPointerException("Directory name can't be null");
		} 

		File dirFile = new File(dir);
		
		if( false == dirFile.exists() || false == dirFile.isDirectory() ) {
			logger.error(String.format("File %s doesn't exist or is not a directory", dir));
			throw new IOException(String.format("File %s doesn't exist or is not a directory", dir));
		}
		
		collectFileList(dirFile, files);
		
		return files;
	}

	private void collectFileList(File dirFile, List<File> files) {

		logger.info(String.format("Scanning files for directory: %s", dirFile.getAbsolutePath()));
		
		List<File> childDirs = new ArrayList<File>();
		
		for(File f : dirFile.listFiles()) {
			if( f.isDirectory() ) {
				childDirs.add(f);
			} 
				
			logger.info(String.format("Adding file or directory to scan list: %s", f.getAbsolutePath()));
			files.add(f);
		}
		
		for(File dir : childDirs) {
			collectFileList(dir, files);
		}
	}

	@Override
	public List<BackupFileDataPacket> createPacketsForFile(BackupFileDataBatch fileBatch, BackupFileTracker fileTracker) throws Exception {				
		
		if( null == fileBatch ) {
			throw new NullPointerException("BackupFileDataBatch reference can't be null");
		} else if( null == fileTracker ) {
			throw new NullPointerException("BackupFileTracker fileTracker) throws reference can't be null");
		}
		File fileRef = fileTracker.getFileReference();
		
		if (false == fileRef.exists() && false == fileTracker.isFileDeleted() ) {
			throw new Exception(String.format("File %s doesn't exist", fileRef.getAbsolutePath()));
		} 
		
		logger.info(String.format("Storing packets for file or dir: %s", fileRef.getAbsolutePath()));
			
		List<BackupFileDataPacket> lstPackets = new ArrayList<BackupFileDataPacket>();
		
		FileInputStream fis = null;		
		FileOutputStream fos = null;
		int packetSize = this.filePacketSize;
		String baseFileName = String.format("%s%s%s", this.localStagingDir, File.separator, UUID.randomUUID());		
		File tempSaveFile = null;
		File finalSaveFile;
		int byteCount;
		int idx;
		byte[] readBytes;	
		byte[] writeBytes;
		int fileCount; 
		
		FileAction action;
        
        if( fileTracker.isFileDeleted() ) {
        	action = FileAction.Delete;
        } else if ( fileTracker.isFileNew() ) {
        	action = FileAction.Create;
        } else {
        	action = FileAction.Update;
        }
        
		try {
			
			if(fileRef.isDirectory() || fileRef.length() == 0) {
				tempSaveFile = new File(String.format("%s.0", baseFileName));
				
				logger.info(String.format("Saving zero byte file to: %s", tempSaveFile.getAbsolutePath()));
				
				fos = new FileOutputStream(tempSaveFile);
				fos.close();
				fos = null;
				
				finalSaveFile = new File(String.format("%s.gz",tempSaveFile.getAbsolutePath()));		        
								
				createGZipFileOutput(tempSaveFile, finalSaveFile);
				
				BackupFileDataPacket dataPacket = new BackupFileDataPacket(fileBatch.getFileBatchID(),
																		   fileTracker.getBackupFileTrackerID(),
																		   (int)finalSaveFile.length(),
																		   1,
																		   1,
																		   finalSaveFile.getParent(),
																		   finalSaveFile.getName(),																		   
																		   action
																		);
				
				lstPackets.add(dataPacket);
								
			} else {
				logger.info(String.format("Saving %d bytes before compress to: %s", fileRef.length(), baseFileName));
				
				fis = new FileInputStream(fileRef);				
				idx = 0;
				fileCount = 0;
				
				while(idx < fileRef.length()) {
					tempSaveFile = new File(String.format("%s.%d", baseFileName, fileCount));
						
					readBytes = new byte[packetSize];
					byteCount = fis.read(readBytes);
					
					idx += byteCount;
					
					if(readBytes.length == byteCount) {
						writeBytes = readBytes;
					} else {
						writeBytes = new byte[byteCount];
						
						System.arraycopy(readBytes, 0, writeBytes, 0, byteCount);
					}
					
					fos = new FileOutputStream(tempSaveFile);
					fos.write(writeBytes);
					
					fos.close();
					fos = null;
					
					finalSaveFile = new File(String.format("%s.gz",tempSaveFile.getAbsolutePath()));		        
					
					createGZipFileOutput(tempSaveFile, finalSaveFile);
					
					tempSaveFile.delete();
					
			        logger.info(String.format("Completed writing %d bytes to file: %s", finalSaveFile.length(), finalSaveFile.getAbsolutePath()));
			        
			        
			        fileCount += 1;
			        
					BackupFileDataPacket dataPacket = new BackupFileDataPacket(fileBatch.getFileBatchID(),
																			   fileTracker.getBackupFileTrackerID(),
																			   (int)finalSaveFile.length(),
																			   fileCount,
																			   0,
																			   finalSaveFile.getParent(),
																			   finalSaveFile.getName(),																			   
																			   action
																				);
				    lstPackets.add(dataPacket);
				}
			}
			
			for(BackupFileDataPacket packet : lstPackets) {
				packet.setPacketsTotal(lstPackets.size());
				
				this.packetRepo.save(packet);
			}	
	
		} finally {
			if(null != fis) {
				fis.close();
			}
						
			if(null != fos) {
				fos.close();
			}
			
			if( null != tempSaveFile && tempSaveFile.exists() ) {
				try {
					tempSaveFile.delete();
				} catch (Exception e) {}
			}
		}
		
		return lstPackets;
	}


	private void createGZipFileOutput(File sourceFile, File destFile) throws IOException {
		
		GZIPOutputStream gos = null;
		FileInputStream gzipIn = null;
		int byteCount;
		byte[] readBytes = new byte[1024];
		
		logger.info(String.format("Saving compressed data to final file: %s", destFile.getAbsolutePath()));
		
		try
		{
			gos = new GZIPOutputStream(new FileOutputStream(destFile));
		
		    gzipIn = new FileInputStream(sourceFile);
		    
		    while ((byteCount = gzipIn.read(readBytes)) > 0) {
		    	gos.write(readBytes, 0, byteCount);
		    }
		    			        
			gos.finish();
			gos.close();
			gos = null;
		
			gzipIn.close();
			gzipIn = null;	
			
		} catch (IOException ex) {		
			throw ex;		
		} finally {
				
			
			if(null != gos) {
				gos.close();
			}
			
			if(null != gzipIn) {
				gzipIn.close();
			}		
		}
	}
	
	@Override 
	public void sendBatchToProcessingQueue(BackupFileDataBatch fileBatch) throws Exception {		
		
		if( null == fileBatch ) {
			throw new NullPointerException("BackupFileDataBatch reference can't be null");
		}
		
		logger.info(String.format("Sending packets to queue for batch ID: %d", fileBatch.getFileBatchID()));
		
		List<BackupFileDataPacket> packetList = packetRepo.findByFileBatchID(fileBatch.getFileBatchID());
		
		if(0 == packetList.size()) {
			logger.info(String.format("No packets found for batch ID: %d, nothing sent", fileBatch.getFileBatchID()));
		} else {		
			logger.info(String.format("Found %d packets to send for batch ID: %d", packetList.size(), fileBatch.getFileBatchID()));
			
			try { 
			
				for(BackupFileDataPacket packet : packetList) {
					this.filePacketHandlerQueue.queuePacket(packet);
				}
				
				fileBatch.setDateTimeSent(new Date());
				
				logger.info(String.format("Sent time of %s set for batch ID: %d", fileBatch.getDateTimeSent(), fileBatch.getFileBatchID()));
			
			} catch (Exception ex) {			
				
				logger.error(String.format("Couldn't queue batch with ID: %d due to exception: %s", 
								fileBatch.getFileBatchID(), ex));
				
				//Remove previous packets from queue
				this.filePacketHandlerQueue.removePacketsForBatch(fileBatch.getFileBatchID());
				
				fileBatch.setDateTimeError(new Date());
				
				fileBatch.setLastSendError(ex.getMessage());
				
				throw ex;
				
			} finally {
				//Remove local files whether or not the batch was successfully sent
				for(BackupFileDataPacket packet : packetList) {
					packet.getFileReference().delete();
				}
				
				//Save update flags
				batchRepo.save(fileBatch);
			}
		}		
	}
	
	@Override
	public List<BackupFileDataPacket> retrieveBatchFromProcessingQueue(Long batchID) throws Exception {
				
		logger.info("Attempting batch retrieve from queue for batch ID: %d", batchID);
		
		List<BackupFileDataBatch> batchList = this.batchRepo.findByFileBatchID(batchID);
		
		if( 0 == batchList.size() ) {
			logger.error("No file batch found for batch ID: %d", batchID);
			
			throw new Exception(String.format("No file batch found for batch ID: %d", batchID));
		}
		
		BackupFileDataBatch fileBatch = batchList.get(0);
		
		if( null != fileBatch.getDateTimeConfirmed() ) {
			logger.error("Can't retrive packets for already confirmed batch with ID: %d", batchID);
			
			throw new IllegalStateException(String.format("Can't retrive packets for already confirmed batch with ID: %d", batchID));
		}	
		
		logger.info(String.format("Retrieving packets from queue for batch ID: %d", batchID));
		
		List<BackupFileDataPacket> packetList = this.filePacketHandlerQueue.retreivePacketsForBatch(batchID);
		
		fileBatch.setDateTimeConfirmed(new Date());
		
		logger.info(String.format("Setting confirm time for batch with ID: %d to: %s", batchID, fileBatch.getDateTimeConfirmed()));
		
		batchRepo.save(fileBatch);
		
		logger.info(String.format("Returning packet list with size: %d for batch ID: %d", packetList.size(), batchID));
		
		return packetList;
		
	}
	
	@Override
	public void removeBatchFromProcessingQueue(Long batchID) throws Exception
	{
		logger.info(String.format("Attempting batch remove from queue for batch ID: %d", batchID));
		
		List<BackupFileDataBatch> batchList = this.batchRepo.findByFileBatchID(batchID);
		
		if( 0 == batchList.size() ) {
			logger.error(String.format("No file batch found for batch ID: %d", batchID));
			
			throw new Exception(String.format("No file batch found for batch ID: %d", batchID));
		}
		
		BackupFileDataBatch fileBatch = batchList.get(0);
		
		logger.info(String.format("Removing packets from queue for batch ID: %d", batchID));
				
		this.filePacketHandlerQueue.removePacketsForBatch(batchID);
				
		fileBatch.setDateTimeSent(null);
		
		batchRepo.save(fileBatch);
		
		logger.info(String.format("All packets removed from queue for batch ID: %d", fileBatch.getFileBatchID()));
		
	}

	
}
