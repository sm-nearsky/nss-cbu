package com.nearskysolutions.cloudbackup.data;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;

public interface BackupFileDataBatchRepository extends CrudRepository<BackupFileDataBatch, Long> {

	List<BackupFileDataBatch> findByFileBatchID(Long fileBatchID);
	
	List<BackupFileDataBatch> findByDateTimeCapturedAfter(Date createDateTime);

}
