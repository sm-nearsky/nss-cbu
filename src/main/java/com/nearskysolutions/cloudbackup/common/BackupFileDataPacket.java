package com.nearskysolutions.cloudbackup.common;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class BackupFileDataPacket {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long fileUpdateID;
	private Long fileBatchID;	
	private int totalBytes;
	private int bytesContained;
	private int packetNumber;	
	private int packetsTotal;
	private String fileDirectory;
	private String fileName;
	private String fileData;
	
	public enum FileAction {
		Create,
		Update,
		Delete,				
	}
	
	private FileAction fileAction;
	
	protected BackupFileDataPacket() { }
	
	public BackupFileDataPacket(Long fileBatchID, int totalBytes, int bytesContained, int packetNumber,
								int packetsTotal, String fileDirectory, String fileName, String fileData, FileAction fileAction) {
				
		this.fileBatchID = fileBatchID;
		this.totalBytes = totalBytes;
		this.bytesContained = bytesContained;
		this.packetNumber = packetNumber;		
		this.packetsTotal = packetsTotal;
		this.fileName = fileName;
		this.fileDirectory = fileDirectory;
		this.fileData = fileData;
		this.fileAction = fileAction;
	}
	
	public Long getFileBatchID() {
		return fileBatchID;
	}
	
	public int getPacketNumber() {
		return packetNumber;
	}

	public void setPacketNumber(int packetNumber) {
		this.packetNumber = packetNumber;
	}

	public int getPacketsTotal() {
		return packetsTotal;
	}

	public void setPacketsTotal(int packetsTotal) {
		this.packetsTotal = packetsTotal;
	}

	public String getFileData() {
		return fileData;
	}

	public void setFileData(String fileData) {
		this.fileData = fileData;
	}	

	public Long getFileUpdateID() {
		return fileUpdateID;
	}

	public void setFileUpdateID(Long fileUpdateID) {
		this.fileUpdateID = fileUpdateID;
	}

	public double getTotalBytes() {
		return totalBytes;
	}

	public void setTotalBytes(int totalBytes) {
		this.totalBytes = totalBytes;
	}

	public int getBytesContained() {
		return bytesContained;
	}

	public void setBytesContained(int bytesContained) {
		this.bytesContained = bytesContained;
	}

	public FileAction getFileAction() {
		return fileAction;
	}

	public void setFileAction(FileAction fileAction) {
		this.fileAction = fileAction;
	}
	
	public String getFileDirectory() {
		return fileDirectory;
	}

	public void setFileDirectory(String fileDirectory) {
		this.fileDirectory = fileDirectory;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	@Override
	public String toString() {
		return String.format("BackupFileDataPacket[fileUpdateID=%d, totalBytes=%d, bytesContained=%d, "+
								"packetNumber=%d, packetsTotal=%d, fileDirectory=%s, fileName=%s, fileAction=%s "+
								"fileData.length=%d]", 
								fileUpdateID, totalBytes, bytesContained, packetNumber, packetsTotal, 
								fileDirectory, fileName, fileAction, (fileData == null ? "null" : fileData.length()));
	}	
	 
}
