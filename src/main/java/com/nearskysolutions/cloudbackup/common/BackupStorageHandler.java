package com.nearskysolutions.cloudbackup.common;

import java.util.List;
import java.util.UUID;

public interface BackupStorageHandler {

	public void retrieveAndProcessBackupPackets(Long batchID);
	
	public void recreateTrackerFiles(UUID clientID, List<Long> trackerIDList, String restoreTarget, boolean isIncludeSubdirectories) throws Exception;
}
