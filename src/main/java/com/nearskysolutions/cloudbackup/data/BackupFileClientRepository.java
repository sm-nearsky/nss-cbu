package com.nearskysolutions.cloudbackup.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;

public interface BackupFileClientRepository extends CrudRepository<BackupFileClient, UUID> {
	
	@Query("SELECT c FROM BackupFileClient c " 
			+ "WHERE "
			+ "c.clientID = :clientID "			
			)
	@Transactional(readOnly=true)
	List<BackupFileClient> findBySingleClientID(@Param("clientID") String clientID);
	
	@Transactional(readOnly=true)
	List<BackupFileClient> findAllByOrderByClientNameAsc();
}
