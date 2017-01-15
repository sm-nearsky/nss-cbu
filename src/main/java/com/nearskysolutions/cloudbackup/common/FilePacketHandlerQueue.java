package com.nearskysolutions.cloudbackup.common;

import java.util.List;

public interface FilePacketHandlerQueue {

	boolean queueHasPackets();
	
	void queuePacket(BackupFileDataPacket packet) throws Exception;
	
	List<BackupFileDataPacket> retreivePacketsForBatch(Long batchID) throws Exception;
	
	void removePacketsForBatch(Long batchID) throws Exception;
	
}
