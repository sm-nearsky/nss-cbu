package com.nearskysolutions.cloudbackup.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@ComponentScan(basePackages="com.nearskysolutions.cloudbackup")
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupClient  implements CommandLineRunner {
	
	@Autowired
	private ApplicationContext appContext;
		
	Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);
        
	public void run(String... args) {
		final CloudBackupClientConfig cbcConfig = appContext.getBean(CloudBackupClientConfig.class);
    
		//logger.info("val " + cbcConfig.getTestValue());
		// save a couple of customers
		TestRunClass trc = appContext.getBean(TestRunClass.class);		
		trc.RunTest();

	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupClient.class, args);
	}	
}
