package com.nearskysolutions.cloudbackup.queue;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;

public interface ClientUpdateHandlerQueue {
	void sendFileTrackerUpdate(BackupFileTracker fileTracker) throws Exception;
	
	void sendBackupFilePacket(BackupFileDataPacket filePacket) throws Exception;
	
	void sendBackupRestoreRequest(BackupRestoreRequest restoreRequest) throws Exception;
}
