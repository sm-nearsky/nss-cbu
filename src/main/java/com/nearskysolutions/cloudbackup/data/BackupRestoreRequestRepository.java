package com.nearskysolutions.cloudbackup.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;

public interface BackupRestoreRequestRepository extends CrudRepository<BackupRestoreRequest, UUID> {

	List<BackupRestoreRequest> findByRequestID(UUID requestID);	
	
	List<BackupRestoreRequest> findAllByOrderBySubmittedDateTimeAsc();
	
//	@Query("SELECT r FROM BackupRetoreRequest r " 
//			+ "WHERE "			
//			+ "LOWER(t.backupRepositoryType) = LOWER(:backupRepositoryType)"
//			+ "AND LOWER(t.backupRepositoryLocation) = LOWER(:backupRepositoryLocation)"
//			+ "AND LOWER(t.backupRepositoryKey) = LOWER(:backupRepositoryKey)"
//			+ "AND LOWER(t.sourceDirectory) = LOWER(:sourceDirectory)"
//			+ "AND LOWER(t.fileName) = LOWER(:fileName)"
//			)
//	List<BackupRestoreRequest> findOpenByOrderBySubmittedDateTimeAsc();
	
	List<BackupRestoreRequest> findBySubmitterId(String submitterId);
	
}
