package com.nearskysolutions.cloudbackup.services;

import java.util.List;
import java.util.UUID;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;

public interface FileHandlerService {
			
	List<BackupFileTracker> updateFileTrackerListing(UUID clientID, 
														 String rootDir,
														 String repoType,
														 String repoLoc,
														 String repoKey) throws Exception;
	
	List<BackupFileDataPacket> createPacketsForFile(BackupFileDataBatch fileBatch, BackupFileTracker fileTracker) throws Exception;
	
	void sendBatchToProcessingQueue(BackupFileDataBatch fileBatch) throws Exception;
	
	List<BackupFileDataPacket> retrieveBatchFromProcessingQueue(Long batchID) throws Exception;
	
	void removeBatchFromProcessingQueue(Long batchID) throws Exception;
}
