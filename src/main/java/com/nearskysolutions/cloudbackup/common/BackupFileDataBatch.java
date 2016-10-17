package com.nearskysolutions.cloudbackup.common;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;

@Entity
public class BackupFileDataBatch {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long fileBatchID;	
	private UUID clientID;
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Date dateTimeCaptured;	
	@ElementCollection
	private List<String> fileList;
		
	protected BackupFileDataBatch() { }
	
	public BackupFileDataBatch(UUID clientID, List<String> fileList) {				
		this.clientID = clientID;		
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
		
	public List<String> getFileList() {
		return fileList;
	}

	public void setFileList(List<String> fileList) {
		this.fileList = fileList;
	}	

	public Date getDateTimeCaptured() {
		return dateTimeCaptured;
	}
	
	@PrePersist
	void handleDateTimeCaptured() {
		if( this.dateTimeCaptured == null ) {
			this.dateTimeCaptured = new Date();
		}
	}
	
	@Override
	public String toString() {
		return String.format("BackupFileDataBatch[clientID=%s, dateTimeCaptured=%s, fileList.size()=%s]",
								clientID, dateTimeCaptured, (fileList == null ? "null" : fileList.size()));
	}
	
		 
}
