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

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@SpringBootApplication
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupServer  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(CloudBackupServer.class);
			    
	@Autowired
	@Qualifier("BackupStorageHandler")
	private BackupStorageHandler backupStorageHandler;
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	public void run(String... args) {
		
		try {
			//TODO Can't re-run batch on exception		
			this.processBackupPackets();
			
			//this.backupStorageHandler.recreateTrackerFiles();
		} catch (Exception ex) {
			logger.error("Server run failed", ex);
		}
	}

	private void processBackupPackets() throws Exception {
		logger.trace("Starting CloudBackupServer.processBackupPackets");
		
		List<BackupFileDataBatch> batchList = dataSvc.getBatchesPendingConfirm();
			
		logger.info(String.format("Found %d pending packet batches", batchList.size()));
		
		//Process each pending batch
		for(BackupFileDataBatch batch : batchList) {
			
			logger.info(String.format("Processing backup packets for batch ID: %d", batch.getFileBatchID()));
			
			this.backupStorageHandler.retrieveAndProcessBackupPackets(batch.getFileBatchID());
		}		
		
		logger.trace("Finished CloudBackupServer.processBackupPackets");
	}
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupServer.class, args);
	}		
}
