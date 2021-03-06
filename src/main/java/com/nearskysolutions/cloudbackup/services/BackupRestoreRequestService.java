package com.nearskysolutions.cloudbackup.services;

import java.util.List;
import java.util.UUID;

import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;

public interface BackupRestoreRequestService {
		
	BackupRestoreRequest addRestoreRequest(BackupRestoreRequest restoreRequest) throws Exception;
		
	BackupRestoreRequest getRestoreRequestByRequestID(UUID requestID) throws Exception;
	
	BackupRestoreRequest updateRestoreRequest(BackupRestoreRequest restoreRequest) throws Exception;
	
	List<BackupRestoreRequest> getRestoreRequestsBySubmitter(String submitterName);
	
	List<BackupRestoreRequest> getAllRestoreRequests();	
	
	void cancelRestoreRequestForID(UUID requestID) throws Exception;
}
