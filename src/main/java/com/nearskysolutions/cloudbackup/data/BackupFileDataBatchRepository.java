package com.nearskysolutions.cloudbackup.data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface BackupFileDataBatchRepository extends CrudRepository<BackupFileDataBatch, Long> {

	List<BackupFileDataBatch> findByFileBatchID(Long fileBatchID);
	
	List<BackupFileDataBatch> findByDateTimeCapturedAfter(Date createDateTime);
		
	@Query("SELECT b FROM BackupFileDataBatch b " +
			"WHERE b.dateTimeConfirmed IS NULL " +
			"AND b.dateTimeError IS NULL " +
			"ORDER BY b.dateTimeCaptured ASC")
	List<BackupFileDataBatch> findPendingPacketBatches();

}
