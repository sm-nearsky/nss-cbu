package com.nearskysolutions.cloudbackup.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

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
	
	@Value( "${com.nearskysolutions.cloudbackup.general.filePacketSize}" )
	private int filePacketSize;
	private String repoType;
	private String repoLoc;
	private String repoKey;
	private int trackerHandlerThreadCount;
	private int singlePacketHandlerThreadCount;
	private int restoreRequestHandlerThreadCount;
	
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
	
	public int getTrackerHandlerThreadCount() {
		return trackerHandlerThreadCount;
	}

	public void setTrackerHandlerThreadCount(int trackerHandlerThreadCount) {
		this.trackerHandlerThreadCount = trackerHandlerThreadCount;
	}
	
	public int getSinglePacketHandlerThreadCount() {
		return singlePacketHandlerThreadCount;
	}

	public void setSinglePacketHandlerThreadCount(int singlePacketHandlerThreadCount) {
		this.singlePacketHandlerThreadCount = singlePacketHandlerThreadCount;
	}

	public int getRestoreRequestHandlerThreadCount() {
		return restoreRequestHandlerThreadCount;
	}

	public void setRestoreRequestHandlerThreadCount(int restoreRequestHandlerThreadCount) {
		this.restoreRequestHandlerThreadCount = restoreRequestHandlerThreadCount;
	}
}
