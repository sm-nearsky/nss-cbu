package com.nearskysolutions.cloudbackup.common;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name="backup_file_data_batch")
public class BackupFileDataBatch {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long fileBatchID;
	
	@Column(name="client_id", columnDefinition = "BINARY(16)")
	private UUID clientID;
	
	@Column(name="date_time_captured")
	private Date dateTimeCaptured;
	
	@Column(name="date_time_sent")
	private Date dateTimeSent;
	
	@Column(name="date_time_confirmed")
	private Date dateTimeConfirmed;
		
	protected BackupFileDataBatch() { }
	
	public BackupFileDataBatch(UUID clientID) {				
		this.clientID = clientID;
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
	
	@PrePersist
	void handleDateTimeCaptured() {
		if( this.dateTimeCaptured == null ) {
			this.dateTimeCaptured = new Date();
		}
	}
	
	@Override
	public String toString() {
		return String.format("BackupFileDataBatch[clientID=%s, dateTimeCaptured=%s]",
								clientID, dateTimeCaptured);
	}

	public Date getDateTimeSent() {
		return dateTimeSent;
	}

	public void setDateTimeSent(Date dateTimeSent) {
		this.dateTimeSent = dateTimeSent;
	}

	public Date getDateTimeConfirmed() {
		return dateTimeConfirmed;
	}

	public void setDateTimeConfirmed(Date dateTimeConfirmed) {
		this.dateTimeConfirmed = dateTimeConfirmed;
	}
	
		 
}
