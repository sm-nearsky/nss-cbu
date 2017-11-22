package com.nearskysolutions.cloudbackup.services;

import java.util.List;
import java.util.UUID;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;

public interface BackupFileClientService {
		
	BackupFileClient addBackupClient(BackupFileClient backupClient) throws Exception;
	
	void updateBackupClient(BackupFileClient backupClient) throws Exception;
	
	void deleteBackupClient(UUID clientID) throws Exception;
	
	BackupFileClient getBackupClientByClientID(UUID clientID) throws Exception;
	
	List<BackupFileClient> getAllBackupClients();	
}
