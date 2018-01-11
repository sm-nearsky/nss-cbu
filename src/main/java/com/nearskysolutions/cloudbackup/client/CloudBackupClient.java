package com.nearskysolutions.cloudbackup.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

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
	
	private ThreadPoolExecutor packetSendThreadPool;	
		
	private AtomicInteger currentProcessingCount = new AtomicInteger(0);
	private AtomicInteger globalPacketSendCount = new AtomicInteger(0);
	
	public void run(String... args) {
	
		try {
			
			this.restTemplate = new RestTemplate();
			
			packetSendThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.cbcConfig.getPacketSendThreadCount());

			this.scanAndSendBackups();
			
		} catch (Exception ex) {
			logger.error("Unable to process backups due to exception", ex);
		}	
		
		while(0 < currentProcessingCount.get() ) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				logger.error("Error in sleep wait", ex);
			}
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
			
			logger.info(String.format("Using clientId: %s", clientId));
						
			//Use values in client admin service as base when not in test mode
			if( false == cbcConfig.isClientTestMode() ) {
			    backupClient = this.restTemplate.getForObject(String.format("%s/%s",cbcConfig.getBackupClientAdminSvcUrl(), clientId), BackupFileClient.class);
			}
			
			//Override any client attributes with local properties (mainly for testing)
						
			List<String> clientDirectoryIncludes = cbcConfig.getClientDirectoryIncludes();
						
			if(null == clientDirectoryIncludes || 0 == clientDirectoryIncludes.size()) {
				clientDirectoryIncludes = backupClient.getDirectoryIncludes();
			}						
				
			logger.info(String.format("Scanning directory includes for clientId: %s", clientId));
			
			if( null == clientDirectoryIncludes || 0 == clientDirectoryIncludes.size() ) {
							
				logger.info(String.format("No directory includes found for client with UUID: %s", clientId));
				
			} else {
				
				logger.info(String.format("Client with UUID %s configured with %d directory includes",
											clientId, 
											clientDirectoryIncludes.size()));
				
				List<String> trackerFilePathList = new ArrayList<String>();
				
				BackupFileTracker[] trackerArr;
				BackupFileTracker tracker;
				boolean isMoreTrackers = true;
				int trackerListCurrentPage = 0;
				int trackerListPageSize = this.cbcConfig.getTrackerListPageSize();
								
				//Collect all trackers from service
				while(isMoreTrackers) {

					if( cbcConfig.isClientTestMode() ) {
						logger.info("Test mode active, all files being treated as new trackers");
						
						isMoreTrackers = false;
					} else {			    
						
						logger.info(String.format("Calling backup admin service to scan current trackers for client: %s, current page: %d, page size: %d",
													clientId.toString(), trackerListCurrentPage, trackerListPageSize));
						
						trackerArr = this.restTemplate.getForObject(String.format("%s/%s?page=%d&size=%d",
																					cbcConfig.getBackupTrackerAdminSvcUrl(), clientId, trackerListCurrentPage, trackerListPageSize), 
																					BackupFileTracker[].class);			
					
						if( null == trackerArr || 0 == trackerArr.length ) {
							logger.info("No trackers returned, scan completed");
							
							isMoreTrackers = false;
						} else {					
							logger.info(String.format("%d trackers returned for scan page", trackerArr.length));
							
							for(int i = 0; i < trackerArr.length; i++) {
								
								tracker = trackerArr[i];
								
								if( false == tracker.getFileReference().exists() ) {
									
									logger.info(String.format("Marking file for delete: %s", tracker.getFileReference().getAbsolutePath()));
									
									tracker.setFileDeleted(true);

									this.sendPacketsForFile(tracker);
									
								} else if( false == tracker.equalsFile(tracker.getFileReference()) ) {
									
									logger.info(String.format("Found changes for file: %s", tracker.getFileReference().getAbsolutePath()));
									
									tracker.updateFileAttributes(tracker.getFileReference());						
									tracker.setFileChanged(true);
									
									logger.info(String.format("Sending tracker to queue for file: %s", tracker.getFileReference().getAbsolutePath()));
									
									this.clientUpdateHandlerQueue.sendFileTrackerUpdate(tracker);

									this.sendPacketsForFile(tracker);
									
								}	else if( BackupFileTrackerStatus.Pending == tracker.getTrackerStatus() || 
										    BackupFileTrackerStatus.Retry == tracker.getTrackerStatus() ) {
									
										//Send packets for each tracker needing update
										//Note: this state is set after the server processes tracker updates		
										logger.info(String.format("Sending packets for file: %s", tracker.getFileReference().getAbsolutePath()));
											
										this.sendPacketsForFile(tracker);									
								}

								trackerFilePathList.add(tracker.getFileReference().getAbsolutePath());
							}
																						
							trackerListCurrentPage += 1;
						}
					}					
				}
				
				Collections.sort(trackerFilePathList);
				
				for(String directory : clientDirectoryIncludes) {
					logger.info(String.format("Processing file references in root directory %s for client with UUID: %s",							
												directory,
												clientId));
												
					File dirFile = new File(directory);
					
					if( false == dirFile.exists() || false == dirFile.isDirectory() ) {
						logger.error(String.format("File %s doesn't exist or is not a directory", directory));						
					} else {									
						processFileList(dirFile, trackerFilePathList);
					}					
				}					
			}
			
		} catch (Exception ex) {			
			logger.error("Unable to complete tracker scan due to exception:", ex);
		}
	}
	
	private void processFileList(File trackerFile, List<String> trackerFilePathList) throws Exception {

		logger.info(String.format("Processing file: %s fir client %s", trackerFile.getAbsolutePath(), this.cbcConfig.getClientId().toString()));
			
		
		if(trackerFilePathList.contains(trackerFile.getAbsolutePath())) {
			//Existing file found in tracker list, no need to take action or keep in the list
			trackerFilePathList.remove(trackerFile.getAbsolutePath());
		} else {			
			logger.info(String.format("Existing file not found, tracker added: %s", trackerFile.getAbsolutePath()));
			
			BackupFileTracker newTracker = new BackupFileTracker(this.cbcConfig.getClientId(), 
																 this.cbcConfig.getRepoType(), 
																 this.cbcConfig.getRepoLoc(), 
																 this.cbcConfig.getRepoKey(), 
																 trackerFile.getAbsolutePath());
			
			newTracker.setFileChanged(true);
			newTracker.setFileNew(true);
							
			this.clientUpdateHandlerQueue.sendFileTrackerUpdate(newTracker);			
		}
		
		if( trackerFile.isDirectory() ) {						
			for(File file : trackerFile.listFiles()) {
				processFileList(file, trackerFilePathList);
			}
		}
	}
			
	private void sendPacketsForFile(BackupFileTracker fileTracker) throws Exception {
		this.packetSendThreadPool.submit(() -> {
			this.handleSendPackets(fileTracker);
		    return null;
		});	
	}
	
	private void handleSendPackets(BackupFileTracker fileTracker) throws Exception {
		
		if( null == fileTracker ) {
			throw new NullPointerException("BackupFileTracker fileTracker) throws reference can't be null");
		}
		
		File fileRef = fileTracker.getFileReference();
		
		if (false == fileRef.exists() && false == fileTracker.isFileDeleted() ) {
			throw new Exception(String.format("File %s doesn't exist", fileRef.getAbsolutePath()));
		} 
		
		try {
				
			currentProcessingCount.incrementAndGet();
			
			logger.info(String.format("Preparing packets for file or dir: %s", fileRef.getAbsolutePath()));
					
			BackupFileDataPacket dataPacket;
			ByteArrayInputStream bais;
			ByteArrayOutputStream baos;
			int packetSize = this.cbcConfig.getFilePacketSize();
			byte[] readBytes = new byte[packetSize];
			int byteCount;
			int idx;			
			byte[] fileBytes;
			long fileLength;
			int currentPacket; 
			int totalPackets;
			
			FileAction action;
	        
	        if( fileTracker.isFileDeleted() ) {
	        	action = FileAction.Delete;
	        } else if ( fileTracker.isFileNew() ) {
	        	action = FileAction.Create;
	        } else {
	        	action = FileAction.Update;
	        }
	        	
			if(fileRef.isDirectory() || fileRef.length() == 0 || FileAction.Delete == action) {
								
				logger.info(String.format("Saving directory or zero length file for ref: %s", fileRef.getAbsolutePath()));
								
				dataPacket = new BackupFileDataPacket(fileTracker.getBackupFileTrackerID(),
														   0,
														   1,
														   1,													   
														   "",
														   "",
														   action
														);
				
				this.clientUpdateHandlerQueue.sendBackupFilePacket(dataPacket);
								
			} else {
				
				//NOTE: dis could be used to create digest along with read but we have to
				//      do this first so we don't have to store all packets at once.  Doing so
				//      would blow out the heap for files.
				//try(FileInputStream fis = new FileInputStream(fileRef);		
					//	DigestInputStream dis = new DigestInputStream(fis, messageDigest)) {
				
				MessageDigest messageDigest = (MessageDigest)MessageDigest.getInstance("MD5");
				
				try(FileInputStream fis =  new FileInputStream(fileRef)) {
								   
				    int numRead;
	
				    do {
				       numRead = fis.read(readBytes);
				       if (numRead > 0) {
				    	   messageDigest.update(readBytes, 0, numRead);
				       }
				    } while (numRead != -1);	      
				}
				
				String md5Digest = Base64.getEncoder().encodeToString(messageDigest.digest());
			  
				
				fileLength = fileRef.length();
				
				if( fileLength <  packetSize ) {
					totalPackets = 1;
				} else {
					totalPackets = (int)(fileLength / (long)packetSize);
					
					//Likely that file length isn't evenly divisible by
					//packet size so there will be on left over
					if( totalPackets % packetSize != 0 ) {
						totalPackets += 1;
					}
				}
				
				try(FileInputStream fis = new FileInputStream(fileRef)) {
					
					idx = 0;
					currentPacket = 0;
					
					while(idx < fileRef.length()) {
						
						
						
						byteCount = fis.read(readBytes);
						
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
											
				        
				        
				        //Convert bytes to base64
						logger.info(String.format("Converting file bytes to base 64 for fileRef: %s with index: %d", fileTracker.getFileName(), idx));
						
						String encodedBytes = Base64.getEncoder().encodeToString(baos.toByteArray());
						currentPacket += 1;
						
						dataPacket = new BackupFileDataPacket(fileTracker.getBackupFileTrackerID(),
																   encodedBytes.length(),
																   currentPacket,
																   totalPackets,															   															   
																   encodedBytes,
																   md5Digest,
																   action
																);
						
						this.clientUpdateHandlerQueue.sendBackupFilePacket(dataPacket);
						
						//Sleep after configured packet max to let queues catch up
						//and garbage collection occur
						if(globalPacketSendCount.incrementAndGet() > cbcConfig.getPacketSendBeforePause()) {
							logger.info("Backing off packet send after sending max packets");
							Thread.sleep(cbcConfig.getPacketSendPauseSeconds() * 1000);
							
							globalPacketSendCount.set(0);
						}
					}				
				
				}
			}		
		} finally {
			currentProcessingCount.decrementAndGet();
		}
	}
}
