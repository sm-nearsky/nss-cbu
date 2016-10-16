package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;

public interface BackupFileDataService {
	
	BackupFileDataPacket addBackupFileDataPacket(BackupFileDataPacket dataPacket) throws Exception;
	
	BackupFileDataPacket getPacketByFileUpdateID(Long fileUpdateID) throws Exception;
	
	BackupFileDataBatch addBackupFileDataBatch(BackupFileDataBatch dataBatch) throws Exception;
	
	List<BackupFileDataBatch> getBatchesCreatedAfter(Date createDateTime) throws Exception;
	
	BackupFileDataBatch getDataBatchByBatchID(Long batchUpdateID) throws Exception;
}
