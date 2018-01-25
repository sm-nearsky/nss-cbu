package com.nearskysolutions.cloudbackup.common;

import java.util.UUID;

public class BackupFileDataPacket {
	
	private UUID dataPacketID;
	private UUID fileTrackerID;
	private UUID clientID;	
	private int bytesContained;
	private int packetNumber;
	private int packetsTotal;
	private long endingFileByteCount;
	private String packetData;
	private String fileChecksumDigest;
	
	public enum FileAction {
		Create,
		Update,
		Delete,				
	}

	private FileAction fileAction;
	
	protected BackupFileDataPacket() { }
	
	public BackupFileDataPacket(UUID fileTrackerID, UUID clientID, int bytesContained, long endingFileByteCount, 
								int packetNumber, int packetsTotal, String packetData, 
								String fileChecksumDigest, FileAction fileAction) {
		
		this.dataPacketID = UUID.randomUUID();
		
		this.fileTrackerID = fileTrackerID;
		this.clientID = clientID;
		this.bytesContained = bytesContained;
		this.endingFileByteCount = endingFileByteCount;
		this.packetNumber = packetNumber;		
		this.packetsTotal = packetsTotal;	
		this.packetData = packetData;
		this.fileChecksumDigest = fileChecksumDigest;
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
		
	public String getFileChecksumDigest() {
		return fileChecksumDigest;
	}

	public void setFileChecksumDigest(String fileChecksumDigest) {
		this.fileChecksumDigest = fileChecksumDigest;
	}

	public long getEndingFileByteCount() {
		return endingFileByteCount;
	}

	public void setEndingFileByteCount(long endingFileByteCount) {
		this.endingFileByteCount = endingFileByteCount;
	}
	
	public UUID getClientID() {
		return clientID;
	}

	public void setClientID(UUID clientID) {
		this.clientID = clientID;
	}

	
	@Override
	public String toString() {
		return String.format("BackupFileDataPacket[dataPacketID=%s, fileTrackerID=%s, clientID=%s, bytesContained=%d, "+
								"packetNumber=%d, packetsTotal=%d, fileAction=%s, fileData.length=%d " +
								"endingFileByteCount=%d]", 
								dataPacketID, fileTrackerID, clientID, bytesContained, packetNumber, packetsTotal, 
								fileAction, (packetData == null ? 0 : packetData.length()), endingFileByteCount);
	}	
	 
}
