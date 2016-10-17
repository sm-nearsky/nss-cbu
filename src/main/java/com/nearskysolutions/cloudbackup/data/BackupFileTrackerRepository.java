package com.nearskysolutions.cloudbackup.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileTrackerRepository extends CrudRepository<BackupFileTracker, Long> {
	
	List<BackupFileTracker> findByClientID(UUID clientID);
	
	//TODO Need Query?
	@Query("SELECT t FROM BackupFileTracker t WHERE t.clientID = ?1 AND t.fileDeletedDateTime IS NULL")
	List<BackupFileTracker> findByClientIDActiveFilesOnly(UUID clientID);
	
}
