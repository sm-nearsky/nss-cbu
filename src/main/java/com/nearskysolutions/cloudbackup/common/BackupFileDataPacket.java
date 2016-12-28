package com.nearskysolutions.cloudbackup.common;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="backup_file_data_packet")
public class BackupFileDataPacket {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="data_packet_id")
	private Long dataPacketID;
	
	@Column(name="file_batch_id")
	private Long fileBatchID;
	
	@Column(name="total_bytes")
	private int totalBytes;
	
	@Column(name="bytes_contained")
	private int bytesContained;
	
	@Column(name="packet_number")
	private int packetNumber;
	
	@Column(name="packets_total")
	private int packetsTotal;
	
	@Column(name="file_directory")
	private String fileDirectory;
	
	@Column(name="file_name")
	private String fileName;
	
	@Column(name="file_data")
	private String fileData;
	
	public enum FileAction {
		Create,
		Update,
		Delete,				
	}
	
	@Column(name="file_action")
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
	
	public Long getDataPacketID() {
		return dataPacketID;
	}

	public Long getFileBatchID() {
		return fileBatchID;
	}
	
	public void setFileBatchID(Long fileBatchID) {
		this.fileBatchID = fileBatchID;
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
		return String.format("BackupFileDataPacket[dataPacketID=%d, totalBytes=%d, bytesContained=%d, "+
								"packetNumber=%d, packetsTotal=%d, fileDirectory=%s, fileName=%s, fileAction=%s "+
								"fileData.length=%d]", 
								dataPacketID, totalBytes, bytesContained, packetNumber, packetsTotal, 
								fileDirectory, fileName, fileAction, (fileData == null ? "null" : fileData.length()));
	}	
	 
}
