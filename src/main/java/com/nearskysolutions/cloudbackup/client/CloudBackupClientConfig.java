package com.nearskysolutions.cloudbackup.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@ComponentScan(basePackages="com.nearskysolutions.cloudbackup.client,"+
							"com.nearskysolutions.cloudbackup.common,"+			
							"com.nearskysolutions.cloudbackup.queue,"+
							"com.nearskysolutions.cloudbackup.util")
@ImportResource("classpath:bean-client-config.xml")                                   
@ConfigurationProperties(prefix = "com.nearskysolutions.cloudbackup.client")
@PropertySource({"classpath:application-client-${env}.properties"})
public class CloudBackupClientConfig {
		
	@Value( "${com.nearskysolutions.cloudbackup.general.filePacketSize}" )
	private int filePacketSize;
	private List<String> clientDirectoryIncludes;
	private UUID clientId;
	private String repoType;
	private String repoLoc;
	private String repoKey;
	private String backupClientAdminSvcUrl;
	private String backupTrackerAdminSvcUrl;
	private boolean isClientTestMode;
		
	public int getFilePacketSize() {
		return filePacketSize;
	}

	public void setFilePacketSize(int filePacketSize) {
		this.filePacketSize = filePacketSize;
	}
	
	public List<String> getClientDirectoryIncludes() {
		return clientDirectoryIncludes;
	}

	public void setClientDirectoryIncludes(List<String> clientDirectoryIncludes) {
		this.clientDirectoryIncludes = clientDirectoryIncludes;
	}

	public UUID getClientId() {
		return clientId;
	}

	public void setClientId(UUID clientId) {
		this.clientId = clientId;
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
	
	public String getBackupClientAdminSvcUrl() {
		return backupClientAdminSvcUrl;
	}

	public void setBackupClientAdminSvcUrl(String backupClientAdminSvcUrl) {
		this.backupClientAdminSvcUrl = backupClientAdminSvcUrl;
	}

	public String getBackupTrackerAdminSvcUrl() {
		return backupTrackerAdminSvcUrl;
	}

	public void setBackupTrackerAdminSvcUrl(String backupTrackerAdminSvcUrl) {
		this.backupTrackerAdminSvcUrl = backupTrackerAdminSvcUrl;
	}
	
	public boolean isClientTestMode() {
		return isClientTestMode;
	}

	public void setIsClientTestMode(boolean isClientTestMode) {
		this.isClientTestMode = isClientTestMode;
	}

}
