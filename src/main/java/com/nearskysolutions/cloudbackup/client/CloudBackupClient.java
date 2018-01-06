package com.nearskysolutions.cloudbackup.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.queue.ClientUpdateHandlerQueue;
import com.nearskysolutions.cloudbackup.util.FileZipUtils;

@SpringBootApplication
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupClient  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);

	@Autowired
	private CloudBackupClientConfig cbcConfig;
	
	@Autowired
	@Qualifier("ClientUpdateHandlerQueue")
	ClientUpdateHandlerQueue clientUpdateHandlerQueue;
	
	private RestTemplate restTemplate;
	
	public void run(String... args) {
	
		try {
			
			this.restTemplate = new RestTemplate();
			
			this.scanAndSendBackups();
			
		} catch (Exception ex) {
			logger.error("Unable to process backups due to exception", ex);
		}	
			
		System.exit(0);
		
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupClient.class, args);
	}	

	private void scanAndSendBackups() {
			
		try {
			
			BackupFileClient backupClient = null;
			UUID clientId = cbcConfig.getClientId();
		
			if( null == clientId ) {
				throw new Exception("No client ID found in backup configuration properties");
			}
			
			logger.info("Using clientId: %s", clientId);
						
			//Use values in client admin service as base when not in test mode
			if( false == cbcConfig.isClientTestMode() ) {
			    backupClient = this.restTemplate.getForObject(String.format("%s/%s",cbcConfig.getBackupClientAdminSvcUrl(), clientId), BackupFileClient.class);
			}
			
			//Override any client attributes with local properties (mainly for testing)
						
			List<String> clientDirectoryIncludes = cbcConfig.getClientDirectoryIncludes();
						
			if(null == clientDirectoryIncludes || 0 == clientDirectoryIncludes.size()) {
				clientDirectoryIncludes = backupClient.getDirectoryIncludes();
			}
						
			String repoType = (cbcConfig.getRepoType() != null) ? cbcConfig.getRepoType() : backupClient.getCurrentRepositoryType();
			String repoLoc = (cbcConfig.getRepoLoc() != null) ? cbcConfig.getRepoLoc() : backupClient.getCurrentRepositoryLocation();
			String repoKey = (cbcConfig.getRepoKey() != null) ? cbcConfig.getRepoKey() : backupClient.getCurrentRepositoryKey();
			
			if( null == clientDirectoryIncludes || 0 == clientDirectoryIncludes.size() ) {
							
				logger.info("No directory includes found for client with UUID: %s", clientId);
				
			} else {
				
				logger.info("Client with UUID %s configured with %d directory includes",
								clientId, 
								clientDirectoryIncludes.size());
				
				List<BackupFileTracker> trackerFullList = new ArrayList<BackupFileTracker>();
				
				boolean isMoreTrackers = true;
				int trackerListCurrentPage = 0;
				int trackerListPageSize = 100;
				
				//Collect all trackers from service
				while(isMoreTrackers) {

					if( cbcConfig.isClientTestMode() ) {
						isMoreTrackers = false;
					
						//Files always treated as new in testing mode
					} else {			    
						BackupFileTracker[] trackerArr = this.restTemplate.getForObject(String.format("%s/%s?page=%d&size=%d",
																						cbcConfig.getBackupTrackerAdminSvcUrl(), clientId, trackerListCurrentPage, trackerListPageSize), 
																						BackupFileTracker[].class);			
					
						if( null == trackerArr || 0 == trackerArr.length ) {
							isMoreTrackers = false;
						} else {
							trackerFullList.addAll(Arrays.asList(trackerArr));
															
							trackerListCurrentPage += 1;
						}
					}					
				}
				
				for(String directory : clientDirectoryIncludes) {
					logger.info("Processing trackers in root directory %s for client with UUID: %s",							
												directory,
												clientId);
					
					logger.info("Using repo type: %s, repo loc: %s, repo key: %s for client UUID: %s",
									repoType, 
									repoLoc, 
									repoKey,
									clientId);
					
					this.processTrackersForDirectory(clientId, directory, repoType, repoLoc, repoKey, trackerFullList);
				}					
			}
			
		} catch (Exception ex) {			
			logger.error("Unable to complete tracker scan due to exception:", ex);
		}
	}
	
	public void processTrackersForDirectory(UUID clientID, 
											 String rootDir,
											 String repoType,
											 String repoLoc,
											 String repoKey,
											 List<BackupFileTracker> trackerList) throws Exception {
				
		if( null == rootDir ) {
			logger.error("Null root directory argument passed to FileHandlerServiceImpl.updateFileTrackerListing");
			throw new NullPointerException("Root directory name can't be null");
		}
				
		List<File> currentFileList = scanFilesForDirectory(rootDir);
		
		BackupFileTracker foundTracker;				
		
		//Check for new files
		for(File currentFile : currentFileList) {
			foundTracker = null;
			
			for(BackupFileTracker tracker : trackerList) {		
		
				if(currentFile.getAbsolutePath().equalsIgnoreCase(tracker.getFileReference().getAbsolutePath())) {
					logger.info(String.format("Existing file found for tracker at: %s,deleted flag=%s", currentFile.getAbsolutePath(), tracker.isFileDeleted()));
				
					foundTracker = tracker;
					
					break;
				}
			}
			
			if(null == foundTracker) {
				
				logger.info(String.format("Existing file not found, tracker added: %s", currentFile.getAbsolutePath()));
				
				BackupFileTracker newTracker = new BackupFileTracker(clientID, repoType, repoLoc, repoKey, currentFile.getAbsolutePath());
				
				newTracker.setFileChanged(true);
				newTracker.setFileNew(true);
								
				this.clientUpdateHandlerQueue.sendFileTrackerUpdate(newTracker);
				
			} else if( false == foundTracker.equalsFile(foundTracker.getFileReference()) ) {
				
				logger.info(String.format("Found changes for file: %s", foundTracker.getFileReference().getAbsolutePath()));
				
				foundTracker.updateFileAttributes(foundTracker.getFileReference());						
				foundTracker.setFileChanged(true);
				
				logger.info(String.format("Sending tracker to queue for file: %s", foundTracker.getFileReference().getAbsolutePath()));
				
				this.clientUpdateHandlerQueue.sendFileTrackerUpdate(foundTracker);
				
				this.sendPacketsForFile(foundTracker);				
			}
		}
		
		//Check for deleted files that still have trackers
		for(BackupFileTracker tracker : trackerList) {		
		
			if( false == tracker.getFileReference().exists() ) {
				logger.info(String.format("Marking file for delete: %s", tracker.getFileReference().getAbsolutePath()));
				
				tracker.setFileDeleted(true);
				
				this.sendPacketsForFile(tracker);
			} else if( BackupFileTrackerStatus.Pending == tracker.getTrackerStatus() || BackupFileTrackerStatus.Retry == tracker.getTrackerStatus() ) {
				
					// Send packets for each tracker needing update
					//Note: this state is set after the server processes tracker updates		
					logger.info(String.format("Sending packets for file: %s", tracker.getFileReference().getAbsolutePath()));
						
					this.sendPacketsForFile(tracker);
				
			}
		}	
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
		
		//Add root to collection
		files.add(dirFile);
		
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
		
	private List<BackupFileDataPacket> sendPacketsForFile(BackupFileTracker fileTracker) throws Exception {				
		
		if( null == fileTracker ) {
			throw new NullPointerException("BackupFileTracker fileTracker) throws reference can't be null");
		}
		File fileRef = fileTracker.getFileReference();
		
		if (false == fileRef.exists() && false == fileTracker.isFileDeleted() ) {
			throw new Exception(String.format("File %s doesn't exist", fileRef.getAbsolutePath()));
		} 
		
		logger.info(String.format("Storing packets for file or dir: %s", fileRef.getAbsolutePath()));
			
		List<BackupFileDataPacket> lstPackets = new ArrayList<BackupFileDataPacket>();
		
		//FileInputStream fis = null;
		ByteArrayInputStream bais;
		ByteArrayOutputStream baos;
		int packetSize = this.cbcConfig.getFilePacketSize();			
		int byteCount;
		int idx;
		byte[] readBytes;	
		byte[] fileBytes;
		int fileCount; 
		
		FileAction action;
        
        if( fileTracker.isFileDeleted() ) {
        	action = FileAction.Delete;
        } else if ( fileTracker.isFileNew() ) {
        	action = FileAction.Create;
        } else {
        	action = FileAction.Update;
        }
        	
		if(fileRef.isDirectory() || fileRef.length() == 0) {
							
			logger.info(String.format("Saving directory or zero length file for ref: %s", fileRef.getAbsolutePath()));
							
			BackupFileDataPacket dataPacket = new BackupFileDataPacket(fileTracker.getBackupFileTrackerID(),
																	   0,
																	   1,
																	   1,
																	   fileRef.getParent(),
																	   fileRef.getName(),
																	   "",
																	   action
																	);
			
			lstPackets.add(dataPacket);
							
		} else {
			
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			
			try(FileInputStream fis = new FileInputStream(fileRef);		
					DigestInputStream dis = new DigestInputStream(fis, messageDigest)) {
				
				idx = 0;
				fileCount = 0;
				
				while(idx < fileRef.length()) {
											
					readBytes = new byte[packetSize];
					byteCount = dis.read(readBytes);
					
					idx += byteCount;
					
					if(readBytes.length == byteCount) {
						fileBytes = readBytes;
					} else {
						fileBytes = new byte[byteCount];
						
						System.arraycopy(readBytes, 0, fileBytes, 0, byteCount);
					}
					
					bais = new ByteArrayInputStream(fileBytes);
					baos = new ByteArrayOutputStream();
															
					FileZipUtils.CreateZipOutputToStream(bais, baos, String.format("%s_%d-%d", fileTracker.getFileName(), idx-byteCount, idx));
					
					bais.close();
					baos.close();
					
					logger.info(String.format("Completed writing %d bytes to output stream for fileRef: %s with index: %d", byteCount, fileTracker.getFileName(), idx));
										
			        fileCount += 1;
			        
			        //Convert bytes to base64
					logger.info(String.format("Converting file bytes to base 64 for fileRef: %s with index: %d", fileTracker.getFileName(), idx));
					
					String encodedBytes = Base64.getEncoder().encodeToString(baos.toByteArray());
					
					BackupFileDataPacket dataPacket = new BackupFileDataPacket(fileTracker.getBackupFileTrackerID(),
																			   encodedBytes.length(),
																			   fileCount,
																			   0,
																			   fileRef.getParent(),
																			   fileRef.getName(),
																			   encodedBytes,
																			   action
																			);
					
				    lstPackets.add(dataPacket);
				}
			}
			
			fileTracker.setLastDigest(Base64.getEncoder().encodeToString(messageDigest.digest()));
			
			//Send another update just before packets to store checksum
			this.clientUpdateHandlerQueue.sendFileTrackerUpdate(fileTracker);
		}
		
		for(BackupFileDataPacket packet : lstPackets) {
			packet.setPacketsTotal(lstPackets.size());
				
			this.clientUpdateHandlerQueue.sendBackupFilePacket(packet);
		}	
			
		return lstPackets;
	}
}
