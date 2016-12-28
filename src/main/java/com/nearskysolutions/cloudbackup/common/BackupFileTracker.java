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
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="backup_file_data_tracker")
public class BackupFileTracker {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="backup_file_tracker_id")
	private Long backupFileTrackerID;
	
	@Column(name="client_id", columnDefinition = "BINARY(16)")
	private UUID clientID;
	
	@Column(name="backup_repository_type")
	private String backupRepositoryType;
	
	@Column(name="backup_repository_location")
	private String backupRepositoryLocation;
	
	@Column(name="backup_repository_key")
	private String backupRepositoryKey;
	
	@Column(name="file_name")
	private String fileName;
	
	@Column(name="source_directory", nullable=true)
	private String sourceDirectory;
	
	@Column(name="file_deleted")
	private boolean isFileDeleted;
		
	@OneToOne(cascade=CascadeType.ALL) 
	private BackupFileAttributes fileAttributes;
	
	@Transient
	private boolean isFileChanged;
	
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

		if(sourceFile.isDirectory()) {
			this.sourceDirectory = sourceFile.getAbsolutePath();
		} else {
			this.sourceDirectory = sourceFile.getParent();
		}
		
		this.fileName = sourceFile.getName();		
					
		updateFileAttributes(sourceFile);		
	}

	public void updateFileAttributes(File sourceFile) throws IOException {
		//TODO make this configurable
		DosFileAttributes  attr = (DosFileAttributes)Files.readAttributes(Paths.get(sourceFile.getAbsolutePath()), DosFileAttributes .class); 
				
		this.fileAttributes = new BackupFileAttributes(new Date(attr.creationTime().toMillis()), 
														  new Date(attr.lastModifiedTime().toMillis()), 
														  new Date(attr.lastAccessTime().toMillis()), 
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
		
		checkDirectoryState(isDirectory(), sourceDirectory, getFileName());
		
		this.sourceDirectory = sourceDirectory;
	}

	public boolean isFileDeleted() {
		return this.isFileDeleted;
	}

	public void setFileDeleted(boolean fileDeleted) {
		this.isFileDeleted = fileDeleted;
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
	
	public String getFullPath() {
	
		if( this.isDirectory() ) {
			return this.getSourceDirectory();
		} else {
			return String.format("%s%s%s", 
								 this.getSourceDirectory(),
								 File.separator,
								 this.getFileName());
		}
	}
	
	@Override
	public String toString() {
		return String.format("BackupFileTracker[clientID=%s, backupRepositoryType=%s, backupRepositoryLocation=%s, " +
								"backupRepositoryKey=%s, fileName=%s, sourceDirectory=%s, " +
								"isFileDeleted=%s, fileAttributes=%s]",
								clientID, backupRepositoryType,	backupRepositoryLocation, backupRepositoryKey,
								fileName, sourceDirectory, isFileDeleted, fileAttributes);
	}

	public boolean isFileChanged() {
		return isFileChanged;
	}

	public void setFileChanged(boolean isFileChanged) {
		this.isFileChanged = isFileChanged;
	}

	public boolean equalsFile(File compareFile) throws IOException {

		boolean filesEqual = 
					compareFile.getParent().equalsIgnoreCase(this.getSourceDirectory()) &&
					compareFile.getName().equalsIgnoreCase(this.getFileName()) &&
					compareFile.isDirectory() == this.isDirectory();
		
		if( true == filesEqual ) {
		
			//TODO make this configurable
			DosFileAttributes  attr = (DosFileAttributes)Files.readAttributes(Paths.get(compareFile.getAbsolutePath()), DosFileAttributes .class); 

			filesEqual = 
					(new Date(attr.creationTime().toMillis())).equals(this.getFileAttributes().getFileCreatedDateTime()) &&
					(new Date(attr.lastModifiedTime().toMillis())).equals(this.getFileAttributes().getFileModifiedDateTime()) &&
					(new Date(attr.lastAccessTime().toMillis())).equals(this.getFileAttributes().getFileAccessDateTime()) &&
					attr.size() == this.getFileAttributes().getFileSize() &&
					attr.size() == this.getFileAttributes().getFileSize() &&
					attr.isRegularFile() == this.getFileAttributes().isRegularFile() &&
					attr.isDirectory() == this.getFileAttributes().isDirectory() &&
					attr.isSymbolicLink() == this.getFileAttributes().isSymbolicLink() &&
					attr.isOther() == this.getFileAttributes().isOther() &&
					attr.isReadOnly() == this.getFileAttributes().isReadOnly() &&
					attr.isHidden() == this.getFileAttributes().isHidden() &&
					attr.isSystem() == this.getFileAttributes().isSystem() &&
					attr.isArchive() == this.getFileAttributes().isArchive();
		
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
