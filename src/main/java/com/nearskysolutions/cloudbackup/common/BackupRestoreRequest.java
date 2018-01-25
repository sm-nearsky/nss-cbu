package com.nearskysolutions.cloudbackup.common;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name="backup_restore_request")
public class BackupRestoreRequest {
	
	@Id
	@GenericGenerator(name = "generator", strategy = "uuid2")
	@GeneratedValue(generator = "generator")	
	@Column(name="request_id", columnDefinition = "uniqueidentifier")		
	private String requestID;
		
	@Column(name="client_id", columnDefinition = "uniqueidentifier")
	private UUID clientID;
	
	@Column(name="submitter_id")
	private String submitterId;
	
	@ElementCollection(fetch = FetchType.EAGER)
	private List<UUID> requestedFileTrackerIDs;
	
	@Column(name="notify_type")
	private NotifyType notifyType;
	
	@Column(name="notify_target")
	private String notifyTarget;
	
	@Column(name="notify_parameter")
	private String notifyParameter;
	
	@Column(name="submitted_date_time")
	private Date submittedDateTime;
	
	@Column(name="processing_start_date_time")
	private Date processingStartDateTime;
	
	@Column(name="completed_date_time")
	private Date completedDateTime;
	
	@Column(name="error_message")
	private String errorMessage;
	
	@Column(name="current_status")
	private RestoreStatus currentStatus;
	
	@Column(name="include_subdirectories")
	private boolean includeSubdirectories;
	
	//@LazyCollection(LazyCollectionOption.FALSE)
	@Fetch(FetchMode.SELECT)	
	@ElementCollection(fetch = FetchType.EAGER)	
	private List<String> restoreResultsURLs;
	
	public enum NotifyType {
		Email,
		None
	};
	
	public enum RestoreStatus {
		Pending,
		Initializing,
		Processsing,
		Success,
		Error,
		Cancelled
	}
	
	protected BackupRestoreRequest() {
		
	}
	
	public BackupRestoreRequest(UUID clientID,
								String submitterId, 
								List<UUID> trackerIds, 
								NotifyType notifyType, 
								String notifyTarget, 
								String notifyParameter,
								boolean includeSubdirectories) throws Exception{
		
		if( null == clientID) {
			throw new Exception("Client ID can't be null");
		}
				
		if( null == submitterId || 0 == submitterId.trim().length()) {
			throw new Exception("Submitter ID can't be null or empty");
		}
		
		if( null == trackerIds || 0 == trackerIds.size()) {
			throw new Exception("Tracker ID list can't be null or empty");
		}
		
		this.clientID = clientID;
		this.submitterId = submitterId;
		this.requestedFileTrackerIDs = trackerIds;
		this.notifyType = notifyType;
		this.notifyTarget = notifyTarget;
		this.notifyParameter = notifyParameter;	
		this.includeSubdirectories = includeSubdirectories;
	}
	
	public UUID getRequestID() {
		return (null != this.requestID ? UUID.fromString(this.requestID) : null);
	}

	public UUID getClientID() {
		return clientID;
	}

	public void setClientID(UUID clientID) {
		this.clientID = clientID;
	}
	
	public String getSubmitterId() {
		return submitterId;
	}

	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
	}

	public NotifyType getNotifyType() {
		return notifyType;
	}

	public void setNotifyType(NotifyType notifyType) {
		this.notifyType = notifyType;
	}

	public String getNotifyTarget() {
		return notifyTarget;
	}

	public void setNotifyTarget(String notifyTarget) {
		this.notifyTarget = notifyTarget;
	}
	
	public String getNotifyParameter() {
		return notifyParameter;
	}

	public void setNotifyParameter(String notifyParameter) {
		this.notifyParameter = notifyParameter;
	}

	public Date getSubmittedDateTime() {
		return submittedDateTime;
	}

	public void setSubmittedDateTime(Date submittedDateTime) {
		this.submittedDateTime = submittedDateTime;
	}

	public Date getProcessingStartDateTime() {
		return processingStartDateTime;
	}

	public void setProcessingStartDateTime(Date processingStartDateTime) {
		this.processingStartDateTime = processingStartDateTime;
	}

	public Date getCompletedDateTime() {
		return completedDateTime;
	}

	public void setCompletedDateTime(Date completedDateTime) {
		this.completedDateTime = completedDateTime;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public boolean isIncludeSubdirectories() {
		return includeSubdirectories;
	}

	public void setIncludeSubdirectories(boolean includeSubdirectories) {
		this.includeSubdirectories = includeSubdirectories;
	}

	public RestoreStatus getCurrentStatus() {
		return currentStatus;
	}

	public void setCurrentStatus(RestoreStatus currentStatus) {
		this.currentStatus = currentStatus;
	}

	public List<String> getRestoreResultsURLs() {
		return restoreResultsURLs;
	}

	public void setRestoreResultsURLs(List<String> restoreResultsURLs) {
		this.restoreResultsURLs = restoreResultsURLs;
	}
	
	public List<UUID> getRequestedFileTrackerIDs() {
		return requestedFileTrackerIDs;
	}

	public void setRequestedFileTrackerIDs(List<UUID> requestedFileTrackerIDs) {
		this.requestedFileTrackerIDs = requestedFileTrackerIDs;
	}	
}
