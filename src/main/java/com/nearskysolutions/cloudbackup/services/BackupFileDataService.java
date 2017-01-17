package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileDataService {
	
	BackupFileDataPacket addBackupFileDataPacket(BackupFileDataPacket dataPacket) throws Exception;
	
	BackupFileDataPacket getPacketByFileDataPacketID(Long dataPacketID) throws Exception;
	
	BackupFileDataBatch addBackupFileDataBatch(BackupFileDataBatch dataBatch) throws Exception;
	
	List<BackupFileDataBatch> getBatchesCreatedAfter(Date createDateTime) throws Exception;
	
	BackupFileDataBatch getDataBatchByBatchID(Long batchUpdateID) throws Exception;
	
	BackupFileTracker addBackupFileTracker(BackupFileTracker fileTracker) throws Exception;
	
	void updateBackupFileTracker(BackupFileTracker fileTracker) throws Exception;
	
	//TODO Create a unit test for this
	BackupFileTracker getTrackerByBackupFileTrackerID(Long trackerID);
	
	List<BackupFileTracker> getAllBackupTrackersForClient(UUID clientID);
	
	List<BackupFileTracker> getActiveBackupTrackersForClient(UUID clientID);
}
