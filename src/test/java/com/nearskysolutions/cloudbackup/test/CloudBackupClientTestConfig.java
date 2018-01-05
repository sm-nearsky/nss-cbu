package com.nearskysolutions.cloudbackup.test;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

@Component
@PropertySource({"classpath:persistence-${env}.properties",
				 "classpath:application-client-${env}.properties"})//,				 
				 //"classpath:application-server.properties"})
@ImportResource("classpath:bean-client-config.xml")
@ComponentScan(basePackages={"com.nearskysolutions.cloudbackup.client",
				 			"com.nearskysolutions.cloudbackup.services",
				 			"com.nearskysolutions.cloudbackup.admin",
				 			"com.nearskysolutions.cloudbackup.util,"+
				 			"com.nearskysolutions.cloudbackup.queue,"})
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupClientTestConfig {
	
}
