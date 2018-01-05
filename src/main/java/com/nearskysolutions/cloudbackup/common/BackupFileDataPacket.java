package com.nearskysolutions.cloudbackup.common;

import java.io.File;
import java.util.UUID;

public class BackupFileDataPacket {
	
	private UUID dataPacketID;
	private UUID fileTrackerID;
	private int bytesContained;
	private int packetNumber;
	private int packetsTotal;
	private String fileDirectory;
	private String fileName;
	private String packetData;
	private File fileRef;
	
	public enum FileAction {
		Create,
		Update,
		Delete,				
	}

	private FileAction fileAction;
	
	protected BackupFileDataPacket() { }
	
	public BackupFileDataPacket(UUID fileTrackerID, int bytesContained, int packetNumber,
								int packetsTotal, String fileDirectory, String fileName, 
								String packetData, FileAction fileAction) {
		
		this.dataPacketID = UUID.randomUUID();
		
		this.dataPacketID = UUID.randomUUID();
		this.fileTrackerID = fileTrackerID;
		this.bytesContained = bytesContained;
		this.packetNumber = packetNumber;		
		this.packetsTotal = packetsTotal;
		this.fileName = fileName;
		this.fileDirectory = fileDirectory;		
		this.packetData = packetData;
		this.fileAction = fileAction;
	}
	
	public UUID getDataPacketID() {
		return dataPacketID;
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
		return packetData;
	}

	public void setFileData(String fileData) {
		this.packetData = fileData;
	}

	public UUID getFileTrackerID() {
		return fileTrackerID;
	}

	public void setFileTrackerID(UUID trackerID) {
		this.fileTrackerID = trackerID;
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
		
		this.fileRef = null;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
		
		this.fileRef = null;
	}
	
	public File getFileReference() {		
		if(null == fileRef) {
			fileRef = new File(String.format("%s%s%s", this.getFileDirectory(), File.separator, this.getFileName()));
		}
		
		return fileRef;
	}
	
	@Override
	public String toString() {
		return String.format("BackupFileDataPacket[dataPacketID=%d, fileTrackerID=%d, bytesContained=%d, "+
								"packetNumber=%d, packetsTotal=%d, fileDirectory=%s, fileName=%s, fileAction=%s "+
								"fileData.length=%d]", 
								dataPacketID, fileTrackerID, bytesContained, packetNumber, packetsTotal, 
								fileDirectory, fileName, fileAction, (packetData == null ? 0 : packetData.length()));
	}	
	 
}
