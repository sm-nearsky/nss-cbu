package com.nearskysolutions.cloudbackup.data;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;

public interface BackupFileDataBatchRepository extends CrudRepository<BackupFileDataBatch, Long> {

	List<BackupFileDataBatch> findByFileBatchID(Long fileBatchID);
	
	//TODO Need Query?
	@Query("SELECT b FROM BackupFileDataBatch b WHERE b.dateTimeCaptured > ?1")
	List<BackupFileDataBatch> findAfterCaptureDateTime(Date createDateTime);

}
