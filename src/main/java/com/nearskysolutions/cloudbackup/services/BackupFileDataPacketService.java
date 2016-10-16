package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;

public interface BackupFileDataPacketService {
	
	BackupFileDataPacket addBackupFileDataPacket(BackupFileDataPacket dataPacket) throws Exception;
	
	BackupFileDataPacket getPacketByFileUpdateID(Long fileUpdateID) throws Exception;
	
	List<BackupFileDataPacket> getPacketsCreatedAfter(Date createDateTime) throws Exception;
	
}
