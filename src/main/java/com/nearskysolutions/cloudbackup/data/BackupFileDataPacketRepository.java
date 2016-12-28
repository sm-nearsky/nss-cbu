package com.nearskysolutions.cloudbackup.data;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;

public interface BackupFileDataPacketRepository extends CrudRepository<BackupFileDataPacket, Long> {

	List<BackupFileDataPacket> findByDataPacketID(Long dataPacketID);	

}
