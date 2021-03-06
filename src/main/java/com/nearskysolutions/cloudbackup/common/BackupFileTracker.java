package com.nearskysolutions.cloudbackup.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.util.Date;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name="backup_file_data_tracker")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BackupFileTracker {
		
	@Id
	@GenericGenerator(name = "generator", strategy = "uuid2")
	@GeneratedValue(generator = "generator")	
	@Column(name="backup_file_tracker_id", columnDefinition = "uniqueidentifier")	
	private String backupFileTrackerID;
	
	@Column(name="client_id", columnDefinition = "uniqueidentifier")
	private UUID clientID;
	
	@Column(name="backup_repository_type")
	private String backupRepositoryType;
	
	@Column(name="backup_repository_location")
	private String backupRepositoryLocation;
	
	@Column(name="backup_repository_key")
	private String backupRepositoryKey;
	
	@Column(name="file_name")
	private String fileName;
	
	@Column(name="file_full_path")
	private String fileFullPath;

	@Column(name="source_directory", nullable=true)
	private String sourceDirectory;
			
	@Column(name="tracker_status")
	private BackupFileTrackerStatus trackerStatus;
	
	@OneToOne(cascade=CascadeType.ALL) 
	private BackupFileAttributes fileAttributes;
	
	@Column(name="last_error", nullable=true)
	private String lastError;
		
	@Column(name="last_status_change_datetime", nullable=true)
	private Date lastStatusChange;
			
	public enum BackupFileTrackerStatus {
		Pending,
		Processing,
		Stored,
		Deleted,		
		Retry,
		Error,
		Unknown
	}
	
	protected BackupFileTracker() { }
	
	public BackupFileTracker(UUID clientID,							 
							String backupRepositoryType,
							String backupRepositoryLocation,
							String backupRepositoryKey,
							String filePath) throws Exception {
				
						
		if(null == filePath) {
			throw new NullPointerException("File path parameter can't be null");
		}
		
		File sourceFile = new File(filePath);
		
		if( false == sourceFile.exists() ) {
			throw new Exception(String.format("File not found for tracking: %s", sourceFile.getAbsolutePath()));
		}
		
		this.clientID = clientID;		
		this.backupRepositoryType = backupRepositoryType;
		this.backupRepositoryLocation = backupRepositoryLocation;
		this.backupRepositoryKey = backupRepositoryKey;
		this.sourceDirectory = sourceFile.getParent();
		
		this.fileName = sourceFile.getName();		
		
		this.trackerStatus = BackupFileTrackerStatus.Unknown;
		
		createFilePathName();
		
		updateFileAttributes(sourceFile);		
	}

	private void createFilePathName() { 
		this.fileFullPath = this.getFileReference().getAbsolutePath();
	}

	public void updateFileAttributes(File sourceFile) throws IOException {
		//TODO make this configurable
		DosFileAttributes  attr = (DosFileAttributes)Files.readAttributes(Paths.get(sourceFile.getAbsolutePath()), DosFileAttributes .class); 
				
		this.fileAttributes = new BackupFileAttributes(attr.creationTime().toMillis(), 
														  attr.lastModifiedTime().toMillis(), 
														  attr.lastAccessTime().toMillis(), 
														  attr.size(), 
														  attr.isRegularFile(), 
														  attr.isDirectory(), 
														  attr.isSymbolicLink(), 
														  attr.isOther(), 
														  (null == attr.fileKey()) ? null : attr.fileKey().toString(), 
														  attr.isReadOnly(),																					   
														  attr.isHidden(), 
														  attr.isSystem(), 
														  attr.isArchive());
	}

	public UUID getBackupFileTrackerID() {
		return (null != this.backupFileTrackerID ? UUID.fromString(this.backupFileTrackerID) : null);
	}
	
	public void setBackupFileTrackerID(UUID backupFileTrackerID) {
		this.backupFileTrackerID = (null != backupFileTrackerID ? backupFileTrackerID.toString() : null);		
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
		
		createFilePathName();
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		
		checkDirectoryState(isDirectory(), sourceDirectory, getFileName());
		
		this.sourceDirectory = sourceDirectory;
		
		createFilePathName();
	}	

	public boolean isDirectory() {
		return (null != this.fileAttributes && true == this.fileAttributes.isDirectory());
	}

	private void checkDirectoryState(boolean isDir, String srcDir, String filePathName) {
		if( true == isDir && 
			null != srcDir &&
			false == srcDir.equalsIgnoreCase(filePathName)) {
			throw new IllegalStateException("Source directory must be null or equal file name for tracked directories");
		}
	}
	
	public BackupFileAttributes getFileAttributes() {
		return fileAttributes;
	}

	public void setFileAttributes(BackupFileAttributes fileAttributes) {
		this.fileAttributes = fileAttributes;
	}	
	
	public File getFileReference() {
		
		String 	pathName = String.format("%s%s%s", 
											 this.getSourceDirectory(),
											 File.separator,
											 this.getFileName());
		
		return new File(pathName);
	}
	
	public BackupFileTrackerStatus getTrackerStatus() {
		return trackerStatus;
	}

	public void setTrackerStatus(BackupFileTrackerStatus trackerStatus) {
		this.trackerStatus = trackerStatus;
	}
	
	public String getLastError() {
		return lastError;
	}

	public void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public Date getLastStatusChange() {
		return lastStatusChange;
	}

	public void setLastStatusChange(Date lastStatusChange) {
		this.lastStatusChange = lastStatusChange;
	}
	
	public Date getLastBytesReceived() {
		return lastStatusChange;
	}
	
	public String getFileFullPath() {
		return fileFullPath;
	}
	
	@Override
	public String toString() {
		
		return String.format("BackupFileTracker[clientID=%s, backupRepositoryType=%s, backupRepositoryLocation=%s, " +
								"backupRepositoryKey=%s, fileName=%s, fileFullPath=%s; sourceDirectory=%s, "+
								"trackerStatus=%s, lastError=%s, lastStatusChange=%s, fileAttributes=%s]",
								clientID, backupRepositoryType,	backupRepositoryLocation, backupRepositoryKey,
								fileName, fileFullPath, sourceDirectory, trackerStatus.toString(), 
								lastError, lastStatusChange,fileAttributes);		
	}

	public boolean equalsFile(File compareFile) throws IOException {

		boolean filesEqual = 
					compareFile.getParent().equalsIgnoreCase(this.getSourceDirectory()) &&
					compareFile.getName().equalsIgnoreCase(this.getFileName()) &&
					compareFile.isDirectory() == this.isDirectory();
		
		if( true == filesEqual ) {
		
			//TODO make this configurable
			DosFileAttributes  attr = (DosFileAttributes)Files.readAttributes(Paths.get(compareFile.getAbsolutePath()), DosFileAttributes .class); 

			//File size can be read incorrectly for directories
			filesEqual = (this.isDirectory() ? filesEqual : attr.size() == this.getFileAttributes().getFileSize());
			
			if(true == filesEqual) {
				filesEqual = 
						(attr.creationTime().toMillis() == this.getFileAttributes().getFileCreatedDateTimeMillis()) &&
						(attr.lastModifiedTime().toMillis() == this.getFileAttributes().getFileModifiedDateTimeMillis()) &&
						(attr.lastAccessTime().toMillis() == this.getFileAttributes().getFileAccessDateTimeMillis()) &&
						attr.isRegularFile() == this.getFileAttributes().isRegularFile() &&
						attr.isDirectory() == this.getFileAttributes().isDirectory() &&
						attr.isSymbolicLink() == this.getFileAttributes().isSymbolicLink() &&
						attr.isOther() == this.getFileAttributes().isOther() &&
						attr.isReadOnly() == this.getFileAttributes().isReadOnly() &&
						attr.isHidden() == this.getFileAttributes().isHidden() &&
						attr.isSystem() == this.getFileAttributes().isSystem() &&
						attr.isArchive() == this.getFileAttributes().isArchive();
			}
		
			if( filesEqual )
			{
				if( attr.fileKey() == null && this.getFileAttributes().getFileKey() != null ||					
					attr.fileKey() != null && this.getFileAttributes().getFileKey() == null 
					) {					
					filesEqual = false;
				} else if( attr.fileKey() != null && this.getFileAttributes().getFileKey() != null ) {
					filesEqual = attr.fileKey().toString().equalsIgnoreCase(this.getFileAttributes().getFileKey());
				} else {
					//Both fileKey values null
				}
			}
		}										
					
		return filesEqual;
	}	
}
