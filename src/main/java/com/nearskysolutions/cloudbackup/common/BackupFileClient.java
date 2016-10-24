package com.nearskysolutions.cloudbackup.common;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class BackupFileClient {
	
	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "BINARY(16)")		
	private UUID clientID;
	
	private String clientName;	
	private String clientDescription;
	private String currentRepositoryType;
	private String currentRepositoryLocation;
	private String currentRepositoryKey;
	
	@ElementCollection(fetch = FetchType.EAGER)	
	private List<String> directoryIncludes;
	
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Date createdDateTime;
		
	protected BackupFileClient() { }
	
	public BackupFileClient(String clientName, 
							String clientDescription, 
							String currentRepositoryType,
							String currentRepositoryLocation,
							String currentRepositoryKey,
							List<String> directoryIncludes) {
				
		this.clientName = clientName;
		this.clientDescription = clientDescription;
		this.currentRepositoryType = currentRepositoryType;
		this.currentRepositoryLocation = currentRepositoryLocation;
		this.currentRepositoryKey = currentRepositoryKey;	
		this.directoryIncludes = directoryIncludes;
	}

	public UUID getClientID() {
		return this.clientID;
	}

	public String getClientName() {
		return this.clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}	
	
	public String getClientDescription() {
		return clientDescription;
	}

	public void setClientDescription(String clientDescription) {
		this.clientDescription = clientDescription;
	}	
	
	public String getCurrentRepositoryType() {
		return currentRepositoryType;
	}

	public void setCurrentRepositoryType(String currentRepositoryType) {
		this.currentRepositoryType = currentRepositoryType;
	}

	public String getCurrentRepositoryLocation() {
		return currentRepositoryLocation;
	}

	public void setCurrentRepositoryLocation(String currentRepositoryLocation) {
		this.currentRepositoryLocation = currentRepositoryLocation;
	}

	public String getCurrentRepositoryKey() {
		return currentRepositoryKey;
	}

	public void setCurrentRepositoryKey(String currentRepositoryKey) {
		this.currentRepositoryKey = currentRepositoryKey;
	}	
	
	public List<String> getDirectoryIncludes() {
		return directoryIncludes;
	}

	public void setDirectoryIncludes(List<String> directoryIncludes) {
		this.directoryIncludes = directoryIncludes;
	}
	
	public Date getCreatedDateTime() {
		return createdDateTime;
	}	
	
	@PrePersist
	void handleDateTimeCreated() {
		if( this.createdDateTime == null ) {
			this.createdDateTime = new Date();
		}
	}
	
	@Override
	public String toString() {
		return String.format("BackupFileClient[clientID=%s, clientName=%s, clientDescription=%s, " +
								"currentRepositoryType=%s, currentRepositoryKey=%s, backupRepositoryKey=%s, " +
								"createdDateTime=%s]",
								clientID, clientName, clientDescription, currentRepositoryType, 
								currentRepositoryLocation, currentRepositoryKey, createdDateTime);
	}		 
}
