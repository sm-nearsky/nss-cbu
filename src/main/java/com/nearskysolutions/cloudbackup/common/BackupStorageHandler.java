package com.nearskysolutions.cloudbackup.common;

public interface BackupStorageHandler {

	public void processBackupPacket(BackupFileDataPacket packet) throws Exception;
		
	public void recreateTrackerFiles(BackupRestoreRequest restoreRequest);
	
}
