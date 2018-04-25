package com.nearskysolutions.cloudbackup.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	private ClientUpdateHandlerQueue clientUpdateHandlerQueue;
	
	private RestTemplate restTemplate;
	
	private Date processingStopTime;
	
	private ThreadPoolExecutor threadBank;
	
	private AtomicInteger messageProcessingCount = new AtomicInteger(0);
	
	public void run(String... args) {
	
		try {
			
			this.restTemplate = new RestTemplate();
			
			Calendar cal = Calendar.getInstance();
			if( this.cbcConfig.getMaxProcessingMinutes() > 0) {
				cal.add(Calendar.MINUTE, this.cbcConfig.getMaxProcessingMinutes());
			} else {
				cal.add(Calendar.MINUTE, 180);
			}
		
			this.processingStopTime = cal.getTime();
					
			this.threadBank = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(1, cbcConfig.getThreadProcessCount()));
			this.threadBank.setKeepAliveTime(2, TimeUnit.SECONDS);
			
			this.scanAndSendBackups();
		
			while(0 < messageProcessingCount.get()) { 
				Thread.sleep(500);
			}
			
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
				int maxProcessingHours = Math.max(12, cbcConfig.getMaxProcessingHours());
								
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
									
									if(BackupFileTrackerStatus.Deleted == tracker.getTrackerStatus()) {
										logger.info(String.format("Skipping deleted file: %s for tracker %s", 
															tracker.getFileReference().getAbsolutePath(),
															tracker.getBackupFileTrackerID().toString()));
									} else {
										logger.info(String.format("Marking file for delete: %s", tracker.getFileReference().getAbsolutePath()));
										
										tracker.setTrackerStatus(BackupFileTrackerStatus.Deleted);
										
										this.sendTrackerUpdate(tracker);										
									}
									
								} else if( false == tracker.equalsFile(tracker.getFileReference()) ) {
									
									if( tracker.getFileReference().length() > cbcConfig.getFileSizeLimitBytes() ) {
										logger.info(String.format("Found changes for file: %s but over size limit so deleting", tracker.getFileReference().getAbsolutePath()));
										
										tracker.setTrackerStatus(BackupFileTrackerStatus.Deleted);

										this.sendTrackerUpdate(tracker);
									} else {									
										logger.info(String.format("Found changes for file: %s", tracker.getFileReference().getAbsolutePath()));
										
										tracker.updateFileAttributes(tracker.getFileReference());						
										tracker.setTrackerStatus(BackupFileTrackerStatus.Pending);				
										
										this.sendTrackerUpdate(tracker);
									}									
								} else if( BackupFileTrackerStatus.Pending == tracker.getTrackerStatus() || 
										   BackupFileTrackerStatus.Retry == tracker.getTrackerStatus() ) {
										     									
									if( BackupFileTrackerStatus.Pending != tracker.getTrackerStatus() ) {										
										tracker.setTrackerStatus(BackupFileTrackerStatus.Pending);
										
										this.sendTrackerUpdate(tracker);
									}
									
									if( tracker.getFileReference().length() > cbcConfig.getFileSizeLimitBytes() ) {
										logger.info(String.format("Found restart state for file: %s but over size limit so deleting", tracker.getFileReference().getAbsolutePath()));
										
										tracker.setTrackerStatus(BackupFileTrackerStatus.Deleted);
	
										this.sendTrackerUpdate(tracker);										
									} else {
										//Send packets for each tracker needing update
										//Note: this state is set after the server processes tracker updates		
										logger.info(String.format("Sending packets for file: %s", tracker.getFileReference().getAbsolutePath()));
										tracker.setTrackerStatus(BackupFileTrackerStatus.Pending);
										
										this.sendPacketsForFile(tracker);
									}
								} else if(BackupFileTrackerStatus.Processing == tracker.getTrackerStatus()) {
																	
									
									Calendar calCurrentTime = Calendar.getInstance();												
									Calendar calLastReceivedTimeAdj = Calendar.getInstance();
									
									calLastReceivedTimeAdj.setTime(tracker.getLastStatusChange());
									calLastReceivedTimeAdj.add(Calendar.HOUR, maxProcessingHours);
									
									//Use in testing to immediately retry
									//calLastReceivedTimeAdj.setTime(new Date(calCurrentTime.getTime().getTime()-1000));
																		
									if( calLastReceivedTimeAdj.before(calCurrentTime) ) {										
										//This update has to make it all the way through before
										//sending packets due to multi-threading									
										tracker.setTrackerStatus(BackupFileTrackerStatus.Pending);										
										this.sendTrackerUpdate(tracker);						
									}
								}

								if( BackupFileTrackerStatus.Deleted != tracker.getTrackerStatus() ) {
									trackerFilePathList.add(tracker.getFileReference().getAbsolutePath());
								}
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

		logger.info(String.format("Processing file: %s for client %s", trackerFile.getAbsolutePath(), this.cbcConfig.getClientId().toString()));
			
		
		if(trackerFilePathList.contains(trackerFile.getAbsolutePath())) {
			//Existing file found in tracker list, no need to take action or keep in the list
			trackerFilePathList.remove(trackerFile.getAbsolutePath());
		} else {			
			if( trackerFile.length() > cbcConfig.getFileSizeLimitBytes() ) {
				logger.info(String.format("Existing file not found, but over byte limit so not sending: %s", trackerFile.getAbsolutePath()));	
			} else {
				logger.info(String.format("Existing file not found, tracker added: %s", trackerFile.getAbsolutePath()));
				
				BackupFileTracker newTracker = new BackupFileTracker(this.cbcConfig.getClientId(), 
																	 this.cbcConfig.getRepoType(), 
																	 this.cbcConfig.getRepoLoc(), 
																	 this.cbcConfig.getRepoKey(), 
																	 trackerFile.getAbsolutePath());
								
				newTracker.setTrackerStatus(BackupFileTrackerStatus.Pending);
				
				this.sendTrackerUpdate(newTracker);
			}
		}
		
		if( trackerFile.isDirectory() && null != trackerFile.listFiles() ) {						
			for(File file : trackerFile.listFiles()) {
				processFileList(file, trackerFilePathList);
			}
		}
	}

			
	private void sendTrackerUpdate(BackupFileTracker tracker) throws Exception {		
				
		this.messageProcessingCount.incrementAndGet();
		
		this.threadBank.submit(() -> {						
			try {
				//Don't allow more messages if past maximum processing time
				Date dtNow = new Date();
				
				if( dtNow.after(this.processingStopTime) ) {
					logger.info(String.format("Past max run time of %d minutes, discarding tracker update send for file tracker ID: %s", 
												this.cbcConfig.getMaxProcessingMinutes(), 
												tracker.getBackupFileTrackerID().toString()));	
				} else {	
					this.clientUpdateHandlerQueue.sendFileTrackerUpdate(tracker);							   
				}
			
			} catch(Exception ex) {
				logger.error("Error in tracker send: ", ex);
			} finally {
				this.messageProcessingCount.decrementAndGet();
			}
			
			return null;
		});		
	}
	
	private void sendPacketsForFile(BackupFileTracker fileTracker) throws Exception {
			
		this.messageProcessingCount.incrementAndGet();
		
		this.threadBank.submit(() -> {	
			try {
			//Don't allow more messages if past maximum processing time
				Date dtNow = new Date();
			
				if( dtNow.after(this.processingStopTime) ) {
					logger.info(String.format("Past max run time of %d minutes, discarding packet send for file tracker ID: %s", 
												this.cbcConfig.getMaxProcessingMinutes(), 
												fileTracker.getBackupFileTrackerID().toString()));	
				} else {	
					this.handleSendPackets(fileTracker);						   
				}
			} catch(Exception ex) {
				logger.error("Error in packet send: ", ex);
			} finally {
				this.messageProcessingCount.decrementAndGet();
			}	
			return null;
		});			
		
	}
	
	private void handleSendPackets(BackupFileTracker fileTracker) throws Exception {
		
		if( null == fileTracker ) {
			throw new NullPointerException("BackupFileTracker fileTracker) throws reference can't be null");
		}
		
		File fileRef = fileTracker.getFileReference();
		
		if (false == fileRef.exists() && (BackupFileTrackerStatus.Deleted != fileTracker.getTrackerStatus()) ) {
			throw new Exception(String.format("File %s doesn't exist", fileRef.getAbsolutePath()));
		} 
		
		try {
						
			logger.info(String.format("Preparing packets for file or dir: %s", fileRef.getAbsolutePath()));
					
			BackupFileDataPacket dataPacket;			
			int packetSize = this.cbcConfig.getFilePacketSize();
			byte[] readBytes = new byte[packetSize];
			String encodedBytes;
			int byteCount;
			long fileByteEndIndex;			
			byte[] fileBytes;
			long fileLength;
			int currentPacket; 
			int totalPackets;
			
			FileAction action;
	        
	        if( BackupFileTrackerStatus.Deleted == fileTracker.getTrackerStatus() ) {
	        	action = FileAction.Delete;
	        } else if ( null == fileTracker.getBackupFileTrackerID() ) {
	        	action = FileAction.Create;
	        } else {
	        	action = FileAction.Update;
	        }
	        	
			if(fileRef.isDirectory() || fileRef.length() == 0 || FileAction.Delete == action) {
								
				logger.info(String.format("Saving directory or zero length file for ref: %s", fileRef.getAbsolutePath()));
								
				dataPacket = new BackupFileDataPacket(fileTracker.getBackupFileTrackerID(),
														cbcConfig.getClientId(),
														   0,
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
	
				    while(0 < (numRead = fis.read(readBytes))) {				       
				    	messageDigest.update(readBytes, 0, numRead);
				    }				       	      
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
					
					fileByteEndIndex = 0;					
					currentPacket = 0;
					
					while(fileByteEndIndex < fileRef.length()) {
						
						byteCount = fis.read(readBytes);
						
						fileByteEndIndex += byteCount;
						
						if(readBytes.length == byteCount) {
							fileBytes = readBytes;
						} else {
							fileBytes = new byte[byteCount];
							
							System.arraycopy(readBytes, 0, fileBytes, 0, byteCount);
						}
						
						try(ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes)) {
							try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
																
								FileZipUtils.CreateZipOutputToStream(bais, baos, String.format("%s_%d-%d", fileTracker.getFileName(), fileByteEndIndex-byteCount, fileByteEndIndex));
						
								logger.info(String.format("Completed writing %d bytes to output stream for fileRef: %s with index: %d", byteCount, fileTracker.getFileName(), fileByteEndIndex));
						        
						        //Convert bytes to base64
								logger.info(String.format("Converting file bytes to base 64 for fileRef: %s with index: %d", fileTracker.getFileName(), fileByteEndIndex));
								
								encodedBytes = Base64.getEncoder().encodeToString(baos.toByteArray());													
							}
						}						
						
						currentPacket += 1;	
						
						dataPacket = new BackupFileDataPacket(fileTracker.getBackupFileTrackerID(),
																   cbcConfig.getClientId(),
																   byteCount,
																   fileByteEndIndex,
																   currentPacket,
																   totalPackets,															   															   
																   encodedBytes,
																   md5Digest,
																   action
																);
						
						logger.info(String.format("Sending packet %d of %d for file: %s, tracker ID: %s",
								dataPacket.getPacketNumber(), dataPacket.getPacketsTotal(), fileTracker.getFileFullPath(), fileTracker.getBackupFileTrackerID()));
						
						this.clientUpdateHandlerQueue.sendBackupFilePacket(dataPacket);										
					}
				}
			}		
		} catch( Exception ex ) {
			logger.error(String.format("Exeption processing packets for file tracker: %s, file path: %s", 
							fileTracker.getBackupFileTrackerID(), fileTracker.getFileFullPath()), ex);
		} 
	}
}
