package com.nearskysolutions.cloudbackup.common;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;

@Entity
public class BackupFileTracker {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long backupFileTrackerID;
	private UUID clientID;
	private String backupRepositoryType;
	private String backupRepositoryLocation;
	private String backupRepositoryKey;
	private String fileName;
	private String sourceDirectory;	
	private Date fileCreatedDateTime;
	private Date fileModifiedDateTime;
	private Date fileDeletedDateTime;
		
	protected BackupFileTracker() { }
	
	public BackupFileTracker(UUID clientID,							 
							String backupRepositoryType,
							String backupRepositoryLocation,
							String backupRepositoryKey,
							String fileName,
							String sourceDirectory,
							Date fileCreatedDateTime,
							Date fileModifiedDateTime,
							Date fileDeletedDateTime) {
				
		this.clientID = clientID;		
		this.backupRepositoryType = backupRepositoryType;
		this.backupRepositoryLocation = backupRepositoryLocation;
		this.backupRepositoryKey = backupRepositoryKey;
		this.fileName = fileName;
		this.fileCreatedDateTime = fileCreatedDateTime;
		this.fileModifiedDateTime = fileModifiedDateTime;
		this.fileDeletedDateTime = fileDeletedDateTime;
	}

	public Long getBackupFileTrackerID() {
		return this.backupFileTrackerID;
	}
	
	public UUID getClientID() {
		return this.clientID;
	}
	
	public String getBackupRepositoryType() {
		return backupRepositoryType;
	}

	public void setBackupRepositoryType(String backupRepositoryType) {
		this.backupRepositoryType = backupRepositoryType;
	}

	public String getBackupRepositoryLocation() {
		return backupRepositoryLocation;
	}

	public void setBackupRepositoryLocation(String backupRepositoryLocation) {
		this.backupRepositoryLocation = backupRepositoryLocation;
	}
	
	public String getBackupRepositoryKey() {
		return backupRepositoryKey;
	}

	public void setBackupRepositoryKey(String backupRepositoryKey) {
		this.backupRepositoryKey = backupRepositoryKey;
	}
			
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public Date getFileCreatedDateTime() {
		return fileCreatedDateTime;
	}
	
	public Date getFileModifiedDateTime() {
		return fileModifiedDateTime;
	}

	public void setFileModifiedDateTime(Date fileModifiedDateTime) {
		this.fileModifiedDateTime = fileModifiedDateTime;
	}

	public Date getFileDeletedDateTime() {
		return fileDeletedDateTime;
	}

	public void setFileDeletedDateTime(Date fileDeletedDateTime) {
		this.fileDeletedDateTime = fileDeletedDateTime;
	}

	@Override
	public String toString() {
		return String.format("BackupFileTracker[clientID=%s, backupRepositoryType=%s, backupRepositoryLocation=%s, " +
								"backupRepositoryKey=%s, fileName=%s, sourceDirectory=%s, fileCreatedDateTime=%s " +
								"fileModifiedDateTime=%s, fileDeletedDateTime=%s]",
								clientID, backupRepositoryType,	backupRepositoryLocation, backupRepositoryKey,
								fileName, sourceDirectory, fileCreatedDateTime, fileModifiedDateTime,
								fileDeletedDateTime);
	}
		 
}
