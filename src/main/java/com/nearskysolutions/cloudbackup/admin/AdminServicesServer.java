package com.nearskysolutions.cloudbackup.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class AdminServicesServer  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(AdminServicesServer.class);
		
	public void run(String... args) {
		
		try {
			logger.info("Starting AdminServiceServer...");
			
		} catch (Exception ex) {
			logger.error("Server run failed", ex);
		}
	}	
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(AdminServicesServer.class, args);
	}		
}
