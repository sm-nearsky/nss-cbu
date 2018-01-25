package com.nearskysolutions.cloudbackup.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;

public interface BackupFileClientRepository extends CrudRepository<BackupFileClient, UUID> {
	
	@Query("SELECT c FROM BackupFileClient c " 
			+ "WHERE "
			+ "c.clientID = :clientID "			
			)
	List<BackupFileClient> findBySingleClientID(@Param("clientID") String clientID);
	
	List<BackupFileClient> findAllByOrderByClientNameAsc();
}
