package com.nearskysolutions.cloudbackup.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.query.Param;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileDataService {
	
	BackupFileTracker addBackupFileTracker(BackupFileTracker fileTracker) throws Exception;
	
	void updateBackupFileTracker(BackupFileTracker fileTracker) throws Exception;
	
	void deleteBackupFileTracker(BackupFileTracker fileTracker) throws Exception;

	BackupFileTracker getTrackerByBackupFileTrackerID(UUID trackerID, UUID clientID);
	
	List<BackupFileTracker> getAllBackupTrackersForClient(UUID clientID);
	
	List<BackupFileTracker> getAllBackupTrackersForClientByPage(UUID clientID, int pageID, int pageSize);
	
	List<BackupFileTracker> getActiveBackupTrackersForClient(UUID clientID);
	
	List<BackupFileTracker> getActiveBackupTrackersForClientByPage(UUID clientID, int pageID, int pageSize);
	
	List<BackupFileTracker> findMatchingTrackers(UUID clientID, 
													String backupRepositoryType, 
													String backupRepositoryLocation,
			 										String backupRepositoryKey, 
			 										String fullFileName);
}
