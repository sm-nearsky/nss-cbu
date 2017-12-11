package com.nearskysolutions.cloudbackup.common;

public interface BackupStorageHandler {

	public void retrieveAndProcessBackupPackets(Long batchID);
		
	public void recreateTrackerFiles(BackupRestoreRequest restoreRequest);
	
}
