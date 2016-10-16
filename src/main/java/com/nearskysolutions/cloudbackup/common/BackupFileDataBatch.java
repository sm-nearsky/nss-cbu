package com.nearskysolutions.cloudbackup.common;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class BackupFileDataBatch {
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long fileBatchID;	
	private UUID clientID;
	private Date dateTimeCaptured;	
	@ElementCollection
	private List<String> fileList;
		
	protected BackupFileDataBatch() { }
	
	public BackupFileDataBatch(UUID clientID, Date dateTimeCaptured, List<String> fileList) {
				
		this.clientID = clientID;
		this.dateTimeCaptured = dateTimeCaptured;
		this.fileList = fileList;
	}

	public Long getFileBatchID() {
		return fileBatchID;		
	}
	
	public UUID getClientID() {
		return clientID;
	}

	public void setClientID(UUID clientID) {
		this.clientID = clientID;
	}
	
	public Date getDateTimeCaptured() {
		return dateTimeCaptured;
	}

	public void setDateTimeCaptured(Date dateTimeCaptured) {
		this.dateTimeCaptured = dateTimeCaptured;
	}

	public List<String> getFileList() {
		return fileList;
	}

	public void setFileList(List<String> fileList) {
		this.fileList = fileList;
	}	
	
	@Override
	public String toString() {
		return String.format("BackupFileDataBatch[clientID=%s, dateTimeCaptured=%s, fileList.size()=%s]",
								clientID, dateTimeCaptured, (fileList == null ? "null" : fileList.size()));
	}
	 
}
