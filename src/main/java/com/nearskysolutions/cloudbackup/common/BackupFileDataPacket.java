package com.nearskysolutions.cloudbackup.common;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class BackupFileDataPacket {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long fileUpdateID;	
	private Date dateTimeCaptured;	
	private int totalBytes;
	private int bytesContained;
	private int packetNumber;	
	private int packetsTotal;	
	private String fileData;
	
	public enum FileAction {
		Create,
		Update,
		Delete,				
	}
	
	private FileAction fileAction;
	
	protected BackupFileDataPacket() { }
	
	public BackupFileDataPacket(int totalBytes, int bytesContained, int packetNumber,
								int packetsTotal, Date dateTimeCaptured, String fileData, FileAction fileAction) {
				
		this.totalBytes = totalBytes;
		this.bytesContained = bytesContained;
		this.packetNumber = packetNumber;		
		this.packetsTotal = packetsTotal;
		this.dateTimeCaptured = dateTimeCaptured;		
		this.fileData = fileData;
		this.fileAction = fileAction;
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

	public Date getDateTimeCaptured() {
		return dateTimeCaptured;
	}

	public void setDateTimeCaptured(Date dateTimeCaptured) {
		this.dateTimeCaptured = dateTimeCaptured;
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
	
	@Override
	public String toString() {
		return String.format("BackupFileDataPacket[fileUpdateID=%d, totalBytes=%d, bytesContained=%d, "+
								"packetNumber=%d, packetsTotal=%d, dateTimeCaptured=%s, fileAction=%s "+
								"fileData.length=%d]", 
								fileUpdateID, totalBytes, bytesContained, packetNumber, packetsTotal, 
								dateTimeCaptured.toString(), fileAction.toString(), fileData.length());
	}
	 
}
