package com.nearskysolutions.cloudbackup.server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.data.BackupFileTrackerRepository;
import com.nearskysolutions.cloudbackup.queue.ClientUpdateMessage;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.BackupRestoreRequestService;
import com.nearskysolutions.cloudbackup.util.JsonConverter;

@EnableJms
@SpringBootApplication
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupServer  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(CloudBackupServer.class);
			    
	@Autowired
	@Qualifier("BackupStorageHandler")
	private BackupStorageHandler backupStorageHandler;
	
	@Autowired 
	private BackupFileDataService fileDataSvc;
	
	@Autowired
	private BackupRestoreRequestService restoreRequestSvc;
		
	public void run(String... args) {
		
		try {
									
			logger.info("Starting CloudBackupServer...");
			
			//Force exit to stop HTTP server
			//System.exit(0);
			
		} catch (Exception ex) {
			logger.error("Server run failed", ex);
			
			System.exit(1);
		}
	}
	
	@JmsListener(destination = "com.nearskysolutions.cloudbackup.queue.nssCbuClientUpdates", containerFactory = "jmsFactory")
    public void receiveMessage(String message) throws Exception {

		logger.trace("In CloudBackupServer.receiveMessage(String message)");
		
		ClientUpdateMessage updateMessage = (ClientUpdateMessage)JsonConverter.ConvertJsonToObject(message, ClientUpdateMessage.class);
        
		logger.info(String.format("Cloud backup message received from queue, message ID: %s and message type: %s",
									updateMessage.getMessageID().toString(), updateMessage.getMessageType().toString()));
		
		switch(updateMessage.getMessageType()) {
			case FileTracker:
				BackupFileTracker tracker = (BackupFileTracker)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupFileTracker.class);        	
	        	logger.info(String.format("Processing add or update for backup file tracker ID: %s", tracker.getBackupFileTrackerID()));        	
	        	this.addOrUpdateFileTracker(tracker);
	        break;	
			case FilePacket:        	
	        	BackupFileDataPacket packet = (BackupFileDataPacket)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupFileDataPacket.class);        	
	        	logger.info(String.format("Processing backup file packet with ID: %s", packet.getDataPacketID().toString()));        	
	        	this.backupStorageHandler.processBackupPacket(packet);
	        break;
			case FileRestore:        	
	        	BackupRestoreRequest restoreRequest = (BackupRestoreRequest)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupRestoreRequest.class);        	
	        	logger.info(String.format("Processing restore request ID: %s", restoreRequest.getRequestID().toString()));
	        	this.processRestoreRequest(restoreRequest);	        	        	        	
	        	break;        	
	        default:
	        	throw new Exception(String.format("Unknown message update type: %s for messsage ID: %s", 
	        			updateMessage.getMessageType(), updateMessage.getMessageID()));	        	
		}
		
		logger.trace("Completed CloudBackupServer.receiveMessage(String message)");
    }
	
	private void addOrUpdateFileTracker(BackupFileTracker newTracker) throws Exception { 
		 
		if( null == newTracker ) {
			logger.error("Null tracker passed to CloudBackupServer.updateFileTrackerListing");
			throw new NullPointerException("tracker can't be null");
		}
		
		logger.trace(String.format("In CloudBackupServer.addOrUpdateFileTracker(BackupFileTracker newTracker): tracker ID=%s", newTracker.getBackupFileTrackerID()));
		
		if( null != newTracker.getBackupFileTrackerID() ) {
			
			BackupFileTracker compareTracker = this.fileDataSvc.getTrackerByBackupFileTrackerID(newTracker.getBackupFileTrackerID());
			
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
			
			newTracker.setFileChanged(true);
			
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
				if( trackerFileList.get(0).isFileDeleted() ) {
					logger.info(String.format("Pre-purging tracker with ID: %d before new create", newTracker.getBackupFileTrackerID()));
					//Delete tracker so a new one will be created
					this.fileDataSvc.deleteBackupFileTracker(trackerFileList.get(0));						
				} else {
					//TODO Verify this works
					newTracker.setBackupFileTrackerID(trackerFileList.get(0).getBackupFileTrackerID());
				}
				
				newTracker.setFileChanged(true);
				
			} else { //0 == trackerFileList.size()
				logger.info(String.format("Adding tracker record for client ID: %s, directory: %s, file: %s", 
											newTracker.getClientID().toString(), 
											newTracker.getSourceDirectory(), 
											newTracker.getFileName()));
				
				newTracker.setFileNew(true);
			}
		}			
		
		if( newTracker.isFileNew() ) {
			this.fileDataSvc.addBackupFileTracker(newTracker);
		} else if( newTracker.isFileChanged() ) {
			this.fileDataSvc.updateBackupFileTracker(newTracker);
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
