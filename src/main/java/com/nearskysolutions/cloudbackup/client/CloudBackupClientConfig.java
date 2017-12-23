package com.nearskysolutions.cloudbackup.client;

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
							"com.nearskysolutions.cloudbackup.data,"+
							"com.nearskysolutions.cloudbackup.services,"+
							"com.nearskysolutions.cloudbackup.prod.beans")
@ImportResource("classpath:bean-client-config.xml")
@ConfigurationProperties(prefix = "com.nearskysolutions.cloudbackup.client")
@PropertySource({"classpath:persistence-${env}.properties",	
				 "classpath:application-client-${env}.properties"})
public class CloudBackupClientConfig {
		
	@Value( "${com.nearskysolutions.cloudbackup.general.filePacketSize}" )
	private int filePacketSize;
	@Value( "${com.nearskysolutions.cloudbackup.general.maxFileProcCount}" )
	private int maxFileProcCount;
	private String packetStagingDir;
	private UUID clientId;
	private String repoType;
	private String repoLoc;
	private String repoKey;
	
	public String getPacketStagingDir() {
		return packetStagingDir;
	}

	public void setPacketStagingDir(String packetStagingDir) {
		this.packetStagingDir = packetStagingDir;
	}

	public int getFilePacketSize() {
		return filePacketSize;
	}

	public void setFilePacketSize(int filePacketSize) {
		this.filePacketSize = filePacketSize;
	}
	
	public int getMaxFileProcCount() {
		return filePacketSize;
	}

	public void setMaxFileProcCount(int maxFileProcCount) {
		this.maxFileProcCount = maxFileProcCount;
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
	
}
