package com.nearskysolutions.cloudbackup.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;

@SpringBootApplication
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupServer  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(CloudBackupServer.class);
			    
	@Autowired
	@Qualifier("BackupStorageHandler")
	private BackupStorageHandler backupStorageHandler;
	
	public void run(String... args) {
		
		this.backupStorageHandler.retrieveAndProcessBackupPackets(1L);
		
		//this.backupStorageHandler.recreateTrackerFiles();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupServer.class, args);
	}		
}
