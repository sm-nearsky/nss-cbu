package com.nearskysolutions.cloudbackup.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileTrackerRepository extends CrudRepository<BackupFileTracker, Long> {
	
	List<BackupFileTracker> findByClientID(UUID clientID);
	
	List<BackupFileTracker> findByClientIDAndIsFileDeletedFalse(UUID clientID);
	
}
