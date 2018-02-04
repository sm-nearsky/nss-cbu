package com.nearskysolutions.cloudbackup.server;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
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
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.queue.ClientUpdateMessage;
import com.nearskysolutions.cloudbackup.queue.JmsHandler;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.util.JsonConverter;

@EnableJms
@EnableCaching
@SpringBootApplication
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupServer  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(CloudBackupServer.class);
			    
	@Autowired
	private CloudBackupServerConfig cbsConfig;
	
	@Autowired
	@Qualifier("BackupStorageHandler")
	private BackupStorageHandler backupStorageHandler;
	
	@Autowired 
	private BackupFileDataService fileDataSvc;
	
	@Autowired
	private JmsHandler jmsHandler;
	
	private boolean inShutdown = false;
	
	private Dictionary<String, ThreadPoolExecutor> threadBank;
		
	private AtomicInteger messageProcessingCount = new AtomicInteger(0);
	
	private final int maxMessageBeforePause = 10000;
	private final int messagePauseSeconds = 5000;
	
	private static final String TRACKER_UPDATE_THREAD_NAME = "com.nearskysolutions.cloudbackup.server.CloudBackupServer.TrackerUpdateThread";
	private static final String RESTORE_REQEUST_THREAD_NAME = "com.nearskysolutions.cloudbackup.server.CloudBackupServer.RestoreRequestThread";
	private static final String SINGLE_PACKET_FILE_UPDATE_THREAD_NAME = "com.nearskysolutions.cloudbackup.server.CloudBackupServer.SingleFilePacketFileUpdateThread";
	private static final String MULTI_PACKET_FILE_UPDATE_THREAD_NAME_PREFIX = "com.nearskysolutions.cloudbackup.server.CloudBackupServer.TrackerUpdateThread_";
	private static final String MESSAGE_PROCESSING_THREADS = "com.nearskysolutions.cloudbackup.server.CloudBackupServer.MessageProcessingThread";
	
	public void run(String... args) {
		
		try {
									
			logger.info("Starting CloudBackupServer...");			
			
			this.threadBank = new Hashtable<String, ThreadPoolExecutor>();
			
			this.threadBank.put(TRACKER_UPDATE_THREAD_NAME, (ThreadPoolExecutor) Executors.newFixedThreadPool(this.cbsConfig.getTrackerHandlerThreadCount()));
			
			this.threadBank.put(RESTORE_REQEUST_THREAD_NAME, (ThreadPoolExecutor) Executors.newFixedThreadPool(this.cbsConfig.getRestoreRequestHandlerThreadCount()));
			
			this.threadBank.put(SINGLE_PACKET_FILE_UPDATE_THREAD_NAME, (ThreadPoolExecutor) Executors.newFixedThreadPool(this.cbsConfig.getSinglePacketHandlerThreadCount()));
			
			//TODO Externalize
			this.threadBank.put(MESSAGE_PROCESSING_THREADS, (ThreadPoolExecutor) Executors.newFixedThreadPool(10));
			
			String threadNameChars = "abcdef1234567890";
			
			for(int i = 0; i < threadNameChars.length(); i++) {			
				for(int j = 0; j < threadNameChars.length(); j++) {
					
					String threadNameKey = getThreadNameForTrackerFileID(String.format("%s%s", threadNameChars.substring(i, i+1), threadNameChars.substring(j, j+1))); 
					
					this.threadBank.put(threadNameKey, (ThreadPoolExecutor) Executors.newFixedThreadPool(1));		
				}
			}

			//TODO Externalize
			this.jmsHandler.setMessageWaitTimeout(500);
			
			//TODO Evaluate loop
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					CloudBackupServer.this.inShutdown = true;
				}				
			});
			
			logger.info("Starting server message loop");
			
			while(false == this.inShutdown ) {
//				this.threadBank.get(MESSAGE_PROCESSING_THREADS).submit(() -> {
					String message = null;
					
	//				try {
						message = this.jmsHandler.waitForMessageOnQueue("nssCbuClientUpdates");
						
						if( message != null ) {
							this.receiveMessage(message);
						} else { //Only check DLQ if not processing standard messages
							message = this.jmsHandler.waitForMessageOnQueue("nssCbuClientUpdates/$DeadLetterQueue");
							
							if( message != null ) {
								this.receiveDLQMessage(message);
							}
						}
						
	//				} catch(javax.jms.IllegalStateException ex) {
	//					if( false == ex.getMessage().contains("The Session is closed") || false == this.inShutdown ) {
	//						logger.error("Receive thread processing erro: ", ex);
	//					}
	//				}
//					return null;
//				});			
			} 
			
			logger.info("Finished server message loop");
			
		} catch (Exception ex) {
			logger.error("Server run failed", ex);
			
			System.exit(1);
		}
	}
		
	private String getThreadNameForTrackerFileID(String trackerFileID) {
		
		return String.format("%s%s", MULTI_PACKET_FILE_UPDATE_THREAD_NAME_PREFIX, trackerFileID.substring(0, 2));
	}
		
	//@JmsListener(destination = "nssCbuClientUpdates", concurrency="1-1")
    public void receiveMessage(String message) {

		logger.trace("In CloudBackupServer.receiveMessage(String message)");
								
		ClientUpdateMessage updateMessage = (ClientUpdateMessage)JsonConverter.ConvertJsonToObject(message, ClientUpdateMessage.class);
        
		logger.info(String.format("Cloud backup message received from queue, message ID: %s and message type: %s",
									updateMessage.getMessageID().toString(), updateMessage.getMessageType().toString()));
		
		try {
			switch(updateMessage.getMessageType()) {
				case FileTracker:
					BackupFileTracker tracker = (BackupFileTracker)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupFileTracker.class);        	
		        	logger.info(String.format("Processing add or update for backup file tracker ID: %s", tracker.getBackupFileTrackerID()));
		        	
//		        	//Tracker update processing can be performed in parallel
//		        	this.threadBank.get(TRACKER_UPDATE_THREAD_NAME).submit(() -> {
		        		this.addOrUpdateFileTracker(tracker);
//					    return null;
//					});
		        	
		        break;	
				case FilePacket:        	
		        	BackupFileDataPacket packet = (BackupFileDataPacket)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupFileDataPacket.class);        	
		        	logger.info(String.format("Processing backup file packet with ID: %s", packet.getDataPacketID().toString()));
		        	
//		        	if( 1 == packet.getPacketsTotal() ) {
//		        		//Files that only require one packet can be handled in parallel
//		        		this.threadBank.get(SINGLE_PACKET_FILE_UPDATE_THREAD_NAME).submit(() -> {
//		        			this.backupStorageHandler.processBackupPacket(packet);
//						    return null;
//						});	        		
//		        	} else {
//		        		//If the file requires more than one packet then handle on one
//		        		//of the alphabetical threads so the assembly remains serialized
//		        		this.threadBank.get(getThreadNameForTrackerFileID(packet.getFileTrackerID().toString())).submit(() -> {
		        			this.backupStorageHandler.processBackupPacket(packet);
//						    return null;
//						});	
//		        	}
		        	
		        break;
				case FileRestore:        	
		        	BackupRestoreRequest restoreRequest = (BackupRestoreRequest)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupRestoreRequest.class);        	
		        	logger.info(String.format("Processing restore request ID: %s", restoreRequest.getRequestID().toString()));
		        	
//		        	//Restore requests can be performed in parallel
//		        	this.threadBank.get(RESTORE_REQEUST_THREAD_NAME).submit(() -> {
		        		this.processRestoreRequest(restoreRequest);
//					    return null;
//					});        	
		        		        	        	        	
		        	break;        	
		        default:
		        	throw new Exception(String.format("Unknown message update type: %s for messsage ID: %s", 
		        			updateMessage.getMessageType(), updateMessage.getMessageID()));	        	
			}
			
//			if(this.messageProcessingCount.incrementAndGet() > this.maxMessageBeforePause) {
//				logger.info("Pausing message processing after reaching max message count");
//				
//				Thread.sleep(this.messagePauseSeconds * 1000);
//				
//				this.messageProcessingCount.set(0);
//			}
			
		} catch(Exception ex) {
			logger.error("Error in message handling", ex);
		}
		
		logger.trace("Completed CloudBackupServer.receiveMessage(String message)");
    }	
		
	//@JmsListener(destination = "nssCbuClientUpdates/$DeadLetterQueue", concurrency="1-5")
    public void receiveDLQMessage(String message) {
//		try {
			logger.info(String.format("Discarding dead letter message: %s", message.substring(0, Math.min(message.length(), 250))));
//			
//			if(this.messageProcessingCount.incrementAndGet() > this.maxMessageBeforePause) {
//				logger.info("Pausing message processing after reaching max message count");
//				
//				Thread.sleep(this.messagePauseSeconds * 1000);
//				
//				this.messageProcessingCount.set(0);
//			}
//		} catch(Exception ex) {
//			logger.error("Error in dead letter message handling", ex);
//		}
	}
	
	private void addOrUpdateFileTracker(BackupFileTracker newTracker) throws Exception { 
				
		if( null == newTracker ) {
			logger.error("Null tracker passed to CloudBackupServer.updateFileTrackerListing");
			throw new NullPointerException("tracker can't be null");
		}
		
		logger.trace(String.format("In CloudBackupServer.addOrUpdateFileTracker(BackupFileTracker newTracker): tracker ID=%s", newTracker.getBackupFileTrackerID()));
						
		if( null != newTracker.getBackupFileTrackerID() ) {
			
			BackupFileTracker compareTracker = this.fileDataSvc.getTrackerByBackupFileTrackerID(newTracker.getBackupFileTrackerID(), newTracker.getClientID());
			
			if( compareTracker == null ) { 
				throw new Exception(String.format("No tracker found to update for ID: %d", newTracker.getBackupFileTrackerID()));
			}
			
			if( false == compareTracker.getClientID().equals(newTracker.getClientID()) ) {
				throw new Exception(String.format("Client ID mismatch for file tracker: %d", newTracker.getBackupFileTrackerID()));
			}
			
			if( false == compareTracker.getBackupRepositoryType().equals(newTracker.getBackupRepositoryType()) ) {
				throw new Exception(String.format("Backup repository type mismatch for file tracker: %d", newTracker.getBackupFileTrackerID()));
			}
			
			if( false == compareTracker.getBackupRepositoryLocation().equals(newTracker.getBackupRepositoryLocation()) ) {
				throw new Exception(String.format("Backup repository location mismatch for file tracker: %d", newTracker.getBackupFileTrackerID()));
			}
			
			if( false == compareTracker.getBackupRepositoryKey().equals(newTracker.getBackupRepositoryKey()) ) {
				throw new Exception(String.format("Backup repository key mismatch for file tracker: %d", newTracker.getBackupFileTrackerID()));
			}
			
			if( false == compareTracker.getSourceDirectory().equals(newTracker.getSourceDirectory()) ) {
				throw new Exception(String.format("Backup source directory mismatch for file tracker: %d", newTracker.getBackupFileTrackerID()));
			}
			
			if( false == compareTracker.getFileName().equals(newTracker.getFileName()) ) {
				throw new Exception(String.format("Backup file name mismatch for file tracker: %d", newTracker.getBackupFileTrackerID()));
			}
			
			logger.info(String.format("Updating tracker record for client ID: %s, directory: %s, file: %s", 
										newTracker.getClientID().toString(), 
										newTracker.getSourceDirectory(), 
										newTracker.getFileName()));	
			
			this.fileDataSvc.updateBackupFileTracker(newTracker);
						
		} else {
		
			List<BackupFileTracker> trackerFileList = this.fileDataSvc.findMatchingTrackers(newTracker.getClientID(), 
																							newTracker.getBackupRepositoryType(), 
																							newTracker.getBackupRepositoryLocation(), 
																							newTracker.getBackupRepositoryKey(), 
																							newTracker.getSourceDirectory(), 
																							newTracker.getFileName());
			
			if( 1 < trackerFileList.size() ) {
				
				throw new Exception(String.format("Found more than one match for tracker with client ID: %s, directory: %s, file: %s", 
													newTracker.getClientID().toString(), 
													newTracker.getSourceDirectory(), 
													newTracker.getFileName()));
			
			}  else if(1 == trackerFileList.size()) {
								
				logger.info(String.format("Updating tracker record for tracker client ID: %s, directory: %s, file: %s",																
											newTracker.getClientID().toString(), 
											newTracker.getSourceDirectory(), 
											newTracker.getFileName()));
				
				//Case where deleted file can be re-created before
				//tracker purge
				if( BackupFileTrackerStatus.Deleted == trackerFileList.get(0).getTrackerStatus() ) {
					logger.info(String.format("Pre-purging tracker with ID: %d before new create", newTracker.getBackupFileTrackerID()));
					//Delete tracker so a new one will be created
					this.fileDataSvc.deleteBackupFileTracker(trackerFileList.get(0));
					
					newTracker.setBackupFileTrackerID(null);										
				} else {
					//TODO Verify this works
					newTracker.setBackupFileTrackerID(trackerFileList.get(0).getBackupFileTrackerID());					
				}
				
			} else { //0 == trackerFileList.size()
				logger.info(String.format("Adding tracker record for client ID: %s, directory: %s, file: %s", 
											newTracker.getClientID().toString(), 
											newTracker.getSourceDirectory(), 
											newTracker.getFileName()));
			}
			
			this.fileDataSvc.addBackupFileTracker(newTracker);
		}			
		
		logger.trace(String.format("Completed CloudBackupServer.addOrUpdateFileTracker(BackupFileTracker newTracker): tracker ID=%s", newTracker.getBackupFileTrackerID()));
	}

	private void processRestoreRequest(BackupRestoreRequest restoreRequest) throws Exception {
				
		if( null == restoreRequest ) {
			logger.error("Null restore request passed to CloudBackupServer.processRestoreRequest");
			throw new NullPointerException("Restore request can't be null");
		}
		
		logger.trace("In CloudBackupServer.processRestoreRequest(BackupRestoreRequest restoreRequest)");
				
		logger.info(String.format("Sending file recreate request to storage handler for request ID: %s", restoreRequest.getRequestID()));
		
		this.backupStorageHandler.recreateTrackerFiles(restoreRequest);
		
		logger.trace("Comleted CloudBackupServer.processRestoreRequest(BackupRestoreRequest restoreRequest)");		
	}
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupServer.class, args);
	}
}
