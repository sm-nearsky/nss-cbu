package com.nearskysolutions.cloudbackup.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.stereotype.Component;

@EnableJms	
@Component
@ComponentScan(basePackages="com.nearskysolutions.cloudbackup.server,"+
							"com.nearskysolutions.cloudbackup.common,"+
							"com.nearskysolutions.cloudbackup.data,"+
							"com.nearskysolutions.cloudbackup.util,"+
							"com.nearskysolutions.cloudbackup.queue,"+
							"com.nearskysolutions.cloudbackup.admin,"+
							"com.nearskysolutions.cloudbackup.services,")
@ConfigurationProperties(prefix = "com.nearskysolutions.cloudbackup.server")
@ImportResource("classpath:bean-server-config.xml")
@PropertySource({"classpath:persistence-${env}.properties",	
					"classpath:application-server-${env}.properties"})
public class CloudBackupServerConfig {
	
	private int maxRestoreSize;
		
	public int getMaxRestoreSize() {
		return maxRestoreSize;
	}

	public void setMaxRestoreSize(int maxRestoreSize) {
		this.maxRestoreSize = maxRestoreSize;
	}
}
