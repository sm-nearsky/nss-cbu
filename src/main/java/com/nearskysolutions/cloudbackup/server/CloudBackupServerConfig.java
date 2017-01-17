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
							"com.nearskysolutions.cloudbackup.services,"+
							"com.nearskysolutions.cloudbackup.prod.beans")
@ConfigurationProperties(prefix = "com.nearskysolutions.cloudbackup.server")
@ImportResource("classpath:bean-config.xml")
@PropertySource({"classpath:persistence-${env}.properties",	
					"classpath:application-server.properties"})
public class CloudBackupServerConfig {
	
	@Value( "${com.nearskysolutions.cloudbackup.general.filePacketSize}" )
	private int filePacketSize;
	private String repoType;
	private String repoLoc;
	private String repoKey;
	private String fileStorageRootDir;
	
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

	public String getFileStorageRootDir() {
		return fileStorageRootDir;
	}

	public void setFileStorageRootDir(String fileStorageRootDir) {
		this.fileStorageRootDir = fileStorageRootDir;
	}
	
}
