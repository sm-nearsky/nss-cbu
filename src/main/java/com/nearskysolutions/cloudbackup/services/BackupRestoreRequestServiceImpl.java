package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.NotifyType;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;
import com.nearskysolutions.cloudbackup.data.BackupRestoreRequestRepository;

@Component
public class BackupRestoreRequestServiceImpl implements BackupRestoreRequestService {
	
	Logger logger = LoggerFactory.getLogger(BackupRestoreRequestServiceImpl.class);
	
	@Autowired
	private BackupRestoreRequestRepository requestRepo;
	
	@Autowired
	private BackupFileClientService backupClientSvc;
	
	@Autowired
	private BackupFileDataService fileSvc;

	
	@Override
	public BackupRestoreRequest addRestoreRequest(BackupRestoreRequest restoreRequest) throws Exception {

		logger.trace("In BackupRestoreRequestServiceImpl.addRestoreRequest(BackupRestoreRequest restoreRequest)");
				
		BackupRestoreRequest retVal = null;
		
		validateBackupRestoreRequest(restoreRequest, "addRestoreRequest");
		
		UUID clientID = restoreRequest.getClientID();
		
		logger.info(String.format("Storing backup request for clientID: %s with notify type: %s", clientID, restoreRequest.getNotifyType()));
		
		for(UUID trackerID : restoreRequest.getRequestedFileTrackerIDs()) {
			BackupFileTracker fileTracker = fileSvc.getTrackerByBackupFileTrackerID(trackerID);
			
			if( fileTracker == null ) {
				throw new Exception(String.format("Couldn't find tracker for ID: %d", trackerID));
			}
			
			if( false == fileTracker.getClientID().equals(clientID) ) {
				throw new Exception(String.format("Client ID mismatch for tracker ID: %d", trackerID));
			}
			
			if( BackupFileTrackerStatus.Stored != fileTracker.getTrackerStatus() ) {
				throw new Exception(String.format("Invalid status for tracker ID: %s, Status must be Stored", trackerID));
			}
			
			logger.info(String.format("Adding file tracker with ID: %s for backup request with clientID: %s", trackerID, clientID));
		}
		
		restoreRequest.setCurrentStatus(RestoreStatus.Pending);
		restoreRequest.setSubmittedDateTime(new Date());
		
		try {
			logger.info("Saving restore request to repo");
			
			retVal = requestRepo.save(restoreRequest);			
			
		} catch(Exception ex) {
			logger.error(String.format("Couldn't save or queue BackupRestoreRequest", restoreRequest.toString()), ex);			
		}
		
		logger.trace("Completed BackupRestoreRequestServiceImpl.addRestoreRequest(BackupRestoreRequest restoreRequest)");
		
		return retVal;
	}

	private void validateBackupRestoreRequest(BackupRestoreRequest restoreRequest, String methodName) throws Exception {
		if( null == restoreRequest ) {
			logger.error(String.format("Null BackupRestoreRequest passed as argument to %s", methodName));
			
			throw new NullPointerException("Backup restore request can't be null");
		}	
		
		UUID clientID = restoreRequest.getClientID();
				
		if( null == clientID) {
			logger.error(String.format("Null client ID passed as argument to %s", methodName));
						
			throw new Exception("BackupRestoreRequest client ID can't be null");
		}
		
		if( null == backupClientSvc.getBackupClientByClientID(clientID)) {
			logger.error(String.format("No backup client found for client ID: %s", clientID));
			
			throw new Exception(String.format("No client found for ID: %s", clientID));
		}
				
		if( null == restoreRequest.getSubmitterId() || 0 == restoreRequest.getSubmitterId().trim().length()) {
			logger.error(String.format("Null or empty submitter ID passed to %s", methodName));
						
			throw new Exception("BackupRestoreRequest submitter ID can't be null or empty");
		}
		
		//TODO Implement e-mail notification
		if( NotifyType.None != restoreRequest.getNotifyType() ) {
			throw new Exception("Only NotifyType.None currently supported"); 
		}		
	}

	@Override
	public BackupRestoreRequest getRestoreRequestByRequestID(UUID requestID) throws Exception {
		
		logger.trace(String.format("In BackupRestoreRequestServiceImpl.getRestoreRequestByRequestID(UUID requestID): requestID=%s", 
				requestID));

		if( requestID == null ) {
			logger.error("Null requestID passed as argument to BackupRestoreRequestServiceImpl.getRestoreRequestByRequestID");
			
			throw new NullPointerException("Request ID can't be null");
		}
		
		logger.info(String.format("Query for BackupRestoreRequest with request ID = %s", requestID));
		
		BackupRestoreRequest retVal;
		List<BackupRestoreRequest> requestList = requestRepo.findByRequestID(requestID);
		
		if( 0 == requestList.size() ) {
			retVal = null;
		} else if( 1 == requestList.size() ) {
			retVal = requestList.get(0);
		} else { //requestList.size() > 1
			throw new Exception(String.format("Invalid count returned for requestID=%s, expected: 1, received: %d", 
								requestID, requestList.size()));
		}
		
		logger.info(String.format("Backup restore request %sfound for ID = %s",((retVal == null) ? "not " : ""), requestID));
		
		logger.trace(String.format("Completed BackupRestoreRequestServiceImpl.getBackupClientByUUID(UUID clientID): clientID=%s with return: %s",
						requestID, retVal));
		
		return retVal;
	}

	@Override
	public List<BackupRestoreRequest> getRestoreRequestsBySubmitter(String submitterId) {
		logger.trace(String.format("In BackupRestoreRequestServiceImpl.getRestoreRequestsBySubmitter(String submitterId): submitterId=%s", 
				submitterId));

		if( submitterId == null ) {
			logger.error("Null submitterName passed as argument to BackupRestoreRequestServiceImpl.getRestoreRequestsBySubmitter");
			
			throw new NullPointerException("Submitter ID can't be null");
		}
		
		logger.info(String.format("Query for BackupRestoreRequest with submitter ID = %s", submitterId));
				
		List<BackupRestoreRequest> retVal = requestRepo.findBySubmitterId(submitterId);
		
		logger.info(String.format("%d backup restore requests found for submitter ID = %s", retVal.size(), submitterId));
		
		logger.trace(String.format("Completed BackupRestoreRequestServiceImpl.getRestoreRequestsBySubmitter(String submitterId): submitterId=%s with %d requests returned",
						submitterId, retVal.size()));
		
		return retVal;
	}

	@Override
	public List<BackupRestoreRequest> getAllRestoreRequests() {

		logger.trace("In BackupRestoreRequestServiceImpl.getAllRestoreRequests()");
		
		logger.info(String.format("Query for all BackupRestoreRequest instances"));
		
		List<BackupRestoreRequest> retVal = requestRepo.findAllByOrderBySubmittedDateTimeAsc();
					
		logger.info(String.format("Query found %d restore requests",retVal.size()));
		
		logger.trace(String.format("Completed BackupRestoreRequestServiceImpl.getAllRestoreRequests(): client list size=%d", retVal.size()));
											
		return retVal;
	}	
	
	@Override
	public void cancelRestoreRequestForID(UUID requestID) throws Exception {
		logger.trace(String.format("In BackupRestoreRequestServiceImpl.cancelRestoreRequestForID(UUID requestID): requestID = %s", requestID));
				
		BackupRestoreRequest restoreRequest = this.getRestoreRequestByRequestID(requestID);
		
		switch(restoreRequest.getCurrentStatus()) {
			case Success:	
			case Cancelled:
			case Error:
				throw new Exception(String.format("Can't cancel request in completed state.  Request with ID: %s currently in state: %s", 
													requestID, restoreRequest.getCurrentStatus()));
			default:
			//status is ok
			break;			
		}
		
		restoreRequest.setCurrentStatus(RestoreStatus.Cancelled);
		restoreRequest.setCompletedDateTime(new Date());
		
		this.requestRepo.save(restoreRequest);
				
		logger.info(String.format("Backup restore request with ID: %s marked for cancellation", requestID));
		
		logger.trace(String.format("Completed BackupRestoreRequestServiceImpl.cancelRestoreRequestForID(UUID requestID): request ID = %s", requestID));
	}

	@Override
	public BackupRestoreRequest updateRestoreRequest(BackupRestoreRequest restoreRequest) throws Exception {
		logger.trace("In BackupRestoreRequestServiceImpl.updateRestoreRequest(BackupRestoreRequest restoreRequest)");
				
		validateBackupRestoreRequest(restoreRequest, "updateRestoreRequest");

		this.requestRepo.save(restoreRequest);
		
		logger.trace(String.format("Completed BackupRestoreRequestServiceImpl.updateRestoreRequest(BackupRestoreRequest restoreRequest): %s", restoreRequest.toString()));
		
		return restoreRequest;
	}
}
