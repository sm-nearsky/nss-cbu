package com.nearskysolutions.cloudbackup.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.FileHandlerService;

@SpringBootApplication
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupClient  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);
	
	@Autowired
	private ApplicationContext appContext;
		
	@Autowired	
	private FileHandlerService fileHandlerSvc;
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	@Autowired 
	private BackupFileClientService clientSvc;
	    
	public void run(String... args) {
		
//		TestRunClass trc = appContext.getBean(TestRunClass.class);		
//		trc.RunTest();

		this.scanAndSendBackups();
		
		System.exit(0);
		
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupClient.class, args);
	}	
	
	private void scanAndSendBackups() {
		final CloudBackupClientConfig cbcConfig = appContext.getBean(CloudBackupClientConfig.class);
		
		UUID clientId = cbcConfig.getClientId();
		
		logger.info("Using clientId: %s", clientId);
				
		try {
		
			BackupFileClient client = clientSvc.getBackupClientByClientID(clientId);
			
			if( null == client ) {
				throw new Exception(String.format("No client found for UUID: %s", clientId));
			}
			
			logger.info("Found client for UUID %s with name: %s", clientId, client.getClientName());
			
			if( 0 == client.getDirectoryIncludes().size() ) {
							
				logger.info("No directory includes found for client with UUID: %s", clientId);
				
			} else {
				
				logger.info("Client with UUID %s configured with %d directory includes",
								clientId, 
								client.getDirectoryIncludes().size());
				
				List<BackupFileTracker> lstTrackers = new ArrayList<BackupFileTracker>();
				
				for(String directory : client.getDirectoryIncludes()) {
					logger.info("Scanning trackers in root directory %s for client with UUID: %s",							
												directory,
												clientId);
					
					logger.info("Using repo type: %s, repo loc: %s, repo key: %s for client UUID: %s",
									cbcConfig.getRepoType(), 
									cbcConfig.getRepoLoc(), 
									cbcConfig.getRepoKey(),
									clientId);
					
					lstTrackers.addAll(fileHandlerSvc.updateFileTrackerListing(client.getClientID(), 
																				directory, 
																				cbcConfig.getRepoType(), 
																				cbcConfig.getRepoLoc(), 
																				cbcConfig.getRepoKey()));
				}	
						
								
				BackupFileDataBatch fileBatch = null;
								
				for(BackupFileTracker tracker : lstTrackers) {
					if( tracker.isFileChanged() ) {
						if( fileBatch == null ) {
							fileBatch = this.dataSvc.addBackupFileDataBatch(new BackupFileDataBatch(clientId));
							
							logger.info("Changed file or files found, creating batch with ID: %d", fileBatch.getFileBatchID());
						}
						
						fileHandlerSvc.createPacketsForFile(fileBatch, tracker);			
					}
				}								
				
				if( fileBatch == null ) {
					logger.info("No changes detected, no batch sent");
				} else {
					
					fileHandlerSvc.sendBatchToProcessingQueue(fileBatch);
					logger.info(String.format("Batch sent with tracker count: %d", lstTrackers.size()));
				}	
			}
			
		} catch (Exception ex) {			
			logger.error("Unable to complete tracker scan due to exception:", ex);
		}
	}
}
