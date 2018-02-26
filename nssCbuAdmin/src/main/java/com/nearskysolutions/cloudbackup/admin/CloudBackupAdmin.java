package com.nearskysolutions.cloudbackup.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class CloudBackupAdmin  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(CloudBackupAdmin.class);
		
	public void run(String... args) {
		
		try {
									
			logger.info("Starting CloudBackupAdmin...");
			
		} catch (Exception ex) {
			logger.error("Admin server run failed", ex);
			
			System.exit(1);
		}
	}	

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupAdmin.class, args);
	}
}
