package com.nearskysolutions.cloudbackup.data;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.NamedQuery;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileTrackerRepository extends PagingAndSortingRepository<BackupFileTracker, UUID> {
	
	@Query("SELECT t FROM BackupFileTracker t " 
			+ "WHERE "
			+ "t.backupFileTrackerID = :trackerID "			
			+ "AND t.clientID = :clientID "
			)	
	@Transactional(readOnly=true)
	List<BackupFileTracker> findBySingleTrackerID(@Param("trackerID") String trackerID, @Param("clientID") UUID clientID);
	
	@Transactional(readOnly=true)
	List<BackupFileTracker> findByClientIDOrderByFileFullPath(@Param("clientID") UUID clientID);
	
	@Transactional(readOnly=true)
	Page<BackupFileTracker> findByClientIDOrderByFileFullPath(@Param("clientID") UUID clientID, Pageable pageable);
	
	@Query("SELECT t FROM BackupFileTracker t " 
			+ "WHERE "
			+ "t.clientID = :clientID "
			+ "AND t.trackerStatus != 3 "
			+ "ORDER BY t.fileFullPath"
			)
	@Transactional(readOnly=true)
	List<BackupFileTracker> findByClientIDAndIsFileDeletedFalse(@Param("clientID") UUID clientID);
	
	
	@Query("SELECT t FROM BackupFileTracker t " 
			+ "WHERE "
			+ "t.clientID = :clientID "
			+ "AND t.trackerStatus != 3 "
			+ "ORDER BY t.fileFullPath"
			)
	@Transactional(readOnly=true)
	Page<BackupFileTracker> findByClientIDAndIsFileDeletedFalse(@Param("clientID") UUID clientID, Pageable pageable);
		
//	@Query("EXECUTE t " 
//			+ "WHERE "
//			+ "t.clientID = :clientID "
//			+ "AND LOWER(t.backupRepositoryType) = LOWER(:backupRepositoryType) "
//			+ "AND LOWER(t.backupRepositoryLocation) = LOWER(:backupRepositoryLocation) "
//			+ "AND LOWER(t.backupRepositoryKey) = LOWER(:backupRepositoryKey) "			
//			+ "AND LOWER(t.fileFullPath) = LOWER(:fileFullPath) "		    
//			)
	//@Procedure("sp_DupTrackerLookup")
	@Query(nativeQuery = true,value = "EXECUTE sp_DupTrackerLookup @ClientId=:clientID "
									  +",@RepositoryType=:backupRepositoryType "
									  +",@RepositoryLocation=:backupRepositoryLocation "
									  +",@RepositoryKey=:backupRepositoryKey "
									  +",@FileFullPath=:fileFullPath")
	@Transactional(readOnly=true)
	List<String> findMatchingTrackers(@Param("clientID") UUID clientID,
									 @Param("backupRepositoryType") String backupRepositoryType,
									 @Param("backupRepositoryLocation") String backupRepositoryLocation,
									 @Param("backupRepositoryKey") String backupRepositoryKey,												 
									 @Param("fileFullPath") String fileFullPath);

}
