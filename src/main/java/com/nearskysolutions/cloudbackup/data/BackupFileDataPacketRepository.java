package com.nearskysolutions.cloudbackup.data;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;

public interface BackupFileDataPacketRepository extends CrudRepository<BackupFileDataPacket, Long> {

	List<BackupFileDataPacket> findByFileUpdateID(Long fileUpdateID);
	
	//TODO Need Query?
	@Query("SELECT p FROM BackupFileDataPacket p WHERE p.dateTimeCaptured > ?1")
	List<BackupFileDataPacket> findAfterCaptureDateTime(Date createDateTime);

}
