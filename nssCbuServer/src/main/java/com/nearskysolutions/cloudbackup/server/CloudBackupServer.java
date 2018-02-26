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
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@EnableCaching
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
	
	public void run(String... args) {
		
		try {
									
			logger.info("Starting CloudBackupServer...");
			
		} catch (Exception ex) {
			logger.error("Server run failed", ex);
			
			System.exit(1);
		}
	}	

	protected void processTrackerUpdate(BackupFileTracker tracker) {
		
		logger.trace("In CloudBackupServer.processTrackerUpdate(BackupFileTracker tracker)");
		
		try {
			logger.info(String.format("Processing add or update for backup file tracker ID: %s", tracker.getBackupFileTrackerID()));
	    	
	   		this.addOrUpdateFileTracker(tracker);
		} catch(Exception ex) {
			logger.error("Error in tracker message handling", ex);
		}
		
		logger.trace("Completed CloudBackupServer.processTrackerUpdate(BackupFileTracker tracker)");
	}
	
	protected void processPacketUpdate(BackupFileDataPacket packet) {
		
		logger.trace("In CloudBackupServer.processPacketUpdate(BackupFileDataPacket packet)");
		
		try {
			logger.info(String.format("Processing backup file packet with ID: %s", packet.getDataPacketID().toString()));
	    	
			this.backupStorageHandler.processBackupPacket(packet);
		} catch(Exception ex) {
			logger.error("Error in packet message handling", ex);
		}
		
		logger.trace("Completed CloudBackupServer.processPacketUpdate(BackupFileDataPacket packet)");
	}
	
	
	protected void processRestoreRequest(BackupRestoreRequest restoreRequest) {
		
		logger.trace("In CloudBackupServer.processRestoreRequest(BackupRestoreRequest restoreRequest)");
				
		logger.info(String.format("Sending file recreate request to storage handler for request ID: %s", restoreRequest.getRequestID()));
		
		this.backupStorageHandler.recreateTrackerFiles(restoreRequest);
				
		logger.trace("Completed CloudBackupServer.processRestoreRequest(BackupRestoreRequest restoreRequest)");
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
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupServer.class, args);
	}
}
