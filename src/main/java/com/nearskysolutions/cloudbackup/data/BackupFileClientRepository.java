package com.nearskysolutions.cloudbackup.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;

public interface BackupFileClientRepository extends CrudRepository<BackupFileClient, UUID> {

	List<BackupFileClient> findByClientID(UUID clientID);	
	
	List<BackupFileClient> findAllByOrderByClientNameAsc();
	
	List<String> findDirectoryIncludesByClientID(UUID clientID);

}
