package com.nearskysolutions.cloudbackup.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(basePackages="com.nearskysolutions.cloudbackup.admin,"+
							"com.nearskysolutions.cloudbackup.common,"+
							"com.nearskysolutions.cloudbackup.data,"+
							"com.nearskysolutions.cloudbackup.util,"+
							"com.nearskysolutions.cloudbackup.queue,"+
							"com.nearskysolutions.cloudbackup.services")
@ConfigurationProperties(prefix = "com.nearskysolutions.cloudbackup.admin")
@ImportResource("classpath:bean-admin-config.xml")
@PropertySource({"classpath:persistence-${env}.properties",	
					"classpath:application-admin-${env}.properties"})
public class AdminServicesConfig {
	
	@Value( "${com.nearskysolutions.cloudbackup.general.filePacketSize}" )
	private int filePacketSize;
	private String repoType;
	private String repoLoc;
	private String repoKey;
	
	public int getFilePacketSize() {
		return filePacketSize;
	}

	public void setFilePacketSize(int filePacketSize) {
		this.filePacketSize = filePacketSize;
	}

	public String getRepoType() {
		return repoType;
	}

	public void setRepoType(String repoType) {
		this.repoType = repoType;
	}

	public String getRepoLoc() {
		return repoLoc;
	}

	public void setRepoLoc(String repoLoc) {
		this.repoLoc = repoLoc;
	}

	public String getRepoKey() {
		return repoKey;
	}

	public void setRepoKey(String repoKey) {
		this.repoKey = repoKey;
	}	
}
