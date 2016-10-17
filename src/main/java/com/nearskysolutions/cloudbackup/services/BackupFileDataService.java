package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileDataService {
	
	BackupFileDataPacket addBackupFileDataPacket(BackupFileDataPacket dataPacket) throws Exception;
	
	BackupFileDataPacket getPacketByFileUpdateID(Long fileUpdateID) throws Exception;
	
	BackupFileDataBatch addBackupFileDataBatch(BackupFileDataBatch dataBatch) throws Exception;
	
	List<BackupFileDataBatch> getBatchesCreatedAfter(Date createDateTime) throws Exception;
	
	BackupFileDataBatch getDataBatchByBatchID(Long batchUpdateID) throws Exception;
	
	BackupFileClient addBackupClient(BackupFileClient backupClient) throws Exception;
	
	void updateBackupClient(BackupFileClient backupClient) throws Exception;
	
	BackupFileClient getBackupClientByUUID(UUID clientID) throws Exception;
	
	List<BackupFileClient> getAllBackupClients();
	
	BackupFileTracker addBackupFileTracker(BackupFileTracker fileTracker) throws Exception;
	
	void updateBackupFileTracker(BackupFileTracker fileTracker) throws Exception;
	
	List<BackupFileTracker> getAllBackupTrackersForClient(UUID clientID);
	
	List<BackupFileTracker> getActiveBackupTrackersForClient(UUID clientID);
}
