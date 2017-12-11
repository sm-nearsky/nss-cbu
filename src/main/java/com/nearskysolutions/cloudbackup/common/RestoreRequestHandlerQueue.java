package com.nearskysolutions.cloudbackup.common;

public interface RestoreRequestHandlerQueue {

	boolean queueHasRequests();
	
	void queueRequest(BackupRestoreRequest restoreRequest) throws Exception;
	
	BackupRestoreRequest retreiveNextRestoreRequest() throws Exception;
}
