package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileClientService {
		
	BackupFileClient addBackupClient(BackupFileClient backupClient) throws Exception;
	
	void updateBackupClient(BackupFileClient backupClient) throws Exception;
	
	BackupFileClient getBackupClientByUUID(UUID clientID) throws Exception;
	
	List<BackupFileClient> getAllBackupClients();	
}
