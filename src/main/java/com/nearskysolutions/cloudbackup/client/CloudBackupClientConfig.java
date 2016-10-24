package com.nearskysolutions.cloudbackup.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "com.nearskysolutions.cloudbackup.client")
@PropertySource({"classpath:persistence-${env}.properties",	
				 "classpath:application.properties"})
public class CloudBackupClientConfig {

	private String packetStagingDir;

	public String getPacketStagingDir() {
		return packetStagingDir;
	}

	public void setPacketStagingDir(String packetStagingDir) {
		this.packetStagingDir = packetStagingDir;
	}
	
	
}
