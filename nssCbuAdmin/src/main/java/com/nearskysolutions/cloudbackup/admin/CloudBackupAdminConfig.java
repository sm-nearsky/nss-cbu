package com.nearskysolutions.cloudbackup.admin;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.stereotype.Component;

@Component
@EnableJms
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
@ComponentScan(basePackages="com.nearskysolutions.cloudbackup.common,"+
							"com.nearskysolutions.cloudbackup.queue,"+
							"com.nearskysolutions.cloudbackup.admin,"+
							"com.nearskysolutions.cloudbackup.services,")
@ConfigurationProperties(prefix = "com.nearskysolutions.cloudbackup.admin")
@PropertySource({"classpath:persistence-${env}.properties",	
					"classpath:application-admin-${env}.properties"})
public class CloudBackupAdminConfig {


}
