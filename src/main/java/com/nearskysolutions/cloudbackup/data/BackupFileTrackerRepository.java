package com.nearskysolutions.cloudbackup.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileTrackerRepository extends CrudRepository<BackupFileTracker, Long> {
	
	List<BackupFileTracker> findByClientID(UUID clientID);
	
	List<BackupFileTracker> findByClientIDAndIsFileDeletedFalse(UUID clientID);
	
	@Query("SELECT t FROM BackupFileTracker t " 
			+ "WHERE "
			+ "t.clientID = :clientID "
			+ "AND LOWER(t.backupRepositoryType) = LOWER(:backupRepositoryType)"
			+ "AND LOWER(t.backupRepositoryLocation) = LOWER(:backupRepositoryLocation)"
			+ "AND LOWER(t.backupRepositoryKey) = LOWER(:backupRepositoryKey)"
			+ "AND LOWER(t.sourceDirectory) = LOWER(:sourceDirectory)"
			+ "AND LOWER(t.fileName) = LOWER(:fileName)"
			)
	List<BackupFileTracker> findMatchingTrackers(@Param("clientID") UUID clientID,
												 @Param("backupRepositoryType") String backupRepositoryType,
												 @Param("backupRepositoryLocation") String backupRepositoryLocation,
												 @Param("backupRepositoryKey") String backupRepositoryKey,
												 @Param("sourceDirectory") String sourceDirectory,
												 @Param("fileName") String fileName);
	
}
