package com.nearskysolutions.cloudbackup.common;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="backup_file_attributes")
public class BackupFileAttributes {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="backup_file_attributes_id")
	private Long backupFileAttributesID;
	
	@Column(name="backup_file_tracker_id")
	private Long backupFileTrackerID;
	
	@Column(name="file_created_date_time")
	private Date fileCreatedDateTime;
	
	@Column(name="file_modified_date_time")
	private Date fileModifiedDateTime;
	
	@Column(name="file_access_date_time")
	private Date fileAccessDateTime;
	
	@Column(name="file_size")
	private Long fileSize;
	
	@Column(name="is_regular_file")
	private boolean isRegularFile;
	
	@Column(name="is_directory")
	private boolean isDirectory;
	
	@Column(name="is_symbolic_link")
	private boolean isSymbolicLink;
		
	@Column(name="is_other")
	private boolean isOther;
	
	@Column(name="file_key")
	private String fileKey;
	
	@Column(name="is_read_only")
	private boolean isReadOnly;
	
	@Column(name="is_hidden")
	private boolean isHidden;
	
	@Column(name="is_system")
	private boolean isSystem;
	
	@Column(name="is_archive")
	private boolean isArchive;
		
	protected BackupFileAttributes() { }
	
	public BackupFileAttributes(Date fileCreatedDateTime,
								Date fileModifiedDateTime,
								Date fileAccessDateTime,
								Long fileSize,
								boolean isRegularFile,
								boolean isDirectory,
								boolean isSymbolicLink,
								boolean isOther,
								String fileKey,
								boolean isReadOnly,
								boolean isHidden,
								boolean isSystem,
								boolean isArchive
								) {
				
		this.setBackupFileTrackerID(backupFileTrackerID);
		this.setFileCreatedDateTime(fileCreatedDateTime);
		this.setFileModifiedDateTime(fileModifiedDateTime); 
		this.setFileAccessDateTime(fileAccessDateTime);
		this.setFileSize(fileSize);
		this.setIsRegularFile(isRegularFile);
		this.setIsDirectory(isDirectory);
		this.setIsSymbolicLink(isSymbolicLink);
		this.setIsOther(isOther);		
		this.setFileKey(fileKey);
		this.setIsReadOnly(isReadOnly);
		this.setIsHidden(isHidden);
		this.setIsSystem(isSystem);
		this.setIsArchive(isArchive);
	}

	public Long getBackupFileAttributesID() {
		return backupFileAttributesID;
	}
	
	public Long getBackupFileTrackerID() {
		return backupFileTrackerID;
	}

	public void setBackupFileTrackerID(Long backupFileTrackerID) {
		this.backupFileTrackerID = backupFileTrackerID;
	}

	public Date getFileCreatedDateTime() {
		return fileCreatedDateTime;
	}

	public void setFileCreatedDateTime(Date fileCreatedDateTime) {
		this.fileCreatedDateTime = fileCreatedDateTime;
	}

	public Date getFileModifiedDateTime() {
		return fileModifiedDateTime;
	}

	public void setFileModifiedDateTime(Date fileModifiedDateTime) {
		this.fileModifiedDateTime = fileModifiedDateTime;
	}

	public Date getFileAccessDateTime() {
		return fileAccessDateTime;
	}

	public void setFileAccessDateTime(Date fileAccessDateTime) {
		this.fileAccessDateTime = fileAccessDateTime;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public boolean isRegularFile() {
		return isRegularFile;
	}

	public void setIsRegularFile(boolean isRegularFile) {
		this.isRegularFile = isRegularFile;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void setIsDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public boolean isSymbolicLink() {
		return isSymbolicLink;
	}

	public void setIsSymbolicLink(boolean isSymbolicLink) {
		this.isSymbolicLink = isSymbolicLink;
	}

	public boolean isOther() {
		return isOther;
	}

	public void setIsOther(boolean isOther) {
		this.isOther = isOther;
	}
	
	public String getFileKey() {
		return fileKey;
	}

	public void setFileKey(String fileKey) {
		this.fileKey = fileKey;
	}
	
	public boolean isReadOnly() {
		return isReadOnly;
	}

	public void setIsReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	public boolean isHidden() {
		return isHidden;
	}

	public void setIsHidden(boolean isHidden) {
		this.isHidden = isHidden;
	}

	public boolean isSystem() {
		return isSystem;
	}

	public void setIsSystem(boolean isSystem) {
		this.isSystem = isSystem;
	}

	public boolean isArchive() {
		return isArchive;
	}

	public void setIsArchive(boolean isArchive) {
		this.isArchive = isArchive;
	}

	@Override
	public String toString() {
		return String.format("BackupFileTracker[backupFileAttributesID=%s, backupFileTrackerID=%s, fileCreatedDateTime=%s, " +
								"fileModifiedDateTime=%s, fileAccessDateTime=%s, fileSize=%s, isRegularFile=%s, " +
								"isDirectory=%s, isSymbolicLink=%s, isOther=%s, fileKey=%s, isReadOnly=%s, isHidden=%s, " +
								"isSystem=%s, isArchive=%s]",
								backupFileAttributesID, backupFileTrackerID, fileCreatedDateTime, fileModifiedDateTime,
								fileAccessDateTime, fileSize, isRegularFile, isDirectory, isSymbolicLink, isOther, fileKey,
								isReadOnly, isHidden, isSystem, isArchive);
	}	
}
