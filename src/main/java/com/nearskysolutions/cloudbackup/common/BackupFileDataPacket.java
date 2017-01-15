package com.nearskysolutions.cloudbackup.common;

import java.io.File;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name="backup_file_data_packet")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class BackupFileDataPacket {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="data_packet_id")
	private Long dataPacketID;
	
	@Column(name="file_batch_id")
	private Long fileBatchID;
	
	@Column(name="file_tracker_id")
	private Long fileTrackerID;
	
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
	
	public enum FileAction {
		Create,
		Update,
		Delete,				
	}
	
	@Column(name="file_action")
	private FileAction fileAction;
	
	@Transient
	private String fileData;
	
	@Transient
	private File fileRef;
	
	protected BackupFileDataPacket() { }
	
	public BackupFileDataPacket(Long fileBatchID, Long fileTrackerID, int bytesContained, int packetNumber,
								int packetsTotal, String fileDirectory, String fileName, FileAction fileAction) {
				
		this.fileBatchID = fileBatchID;
		this.fileTrackerID = fileTrackerID;
		this.bytesContained = bytesContained;
		this.packetNumber = packetNumber;		
		this.packetsTotal = packetsTotal;
		this.fileName = fileName;
		this.fileDirectory = fileDirectory;		
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

	public Long getFileTrackerID() {
		return fileTrackerID;
	}

	public void setFileTrackerID(Long trackerID) {
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
								fileDirectory, fileName, fileAction, (fileData == null ? 0 : fileData.length()));
	}	
	 
}
