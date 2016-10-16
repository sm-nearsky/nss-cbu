package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.data.BackupFileDataPacketRepository;

@Component
public class BackupFileDataPacketServiceImpl implements BackupFileDataPacketService {
	
	Logger logger = LoggerFactory.getLogger( BackupFileDataPacketService.class);
	
	@Autowired
	private BackupFileDataPacketRepository repository;

	@Override
	public BackupFileDataPacket getPacketByFileUpdateID(Long fileUpdateID) throws Exception {
		
		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getPacketByFileUpdateID(Long fileUpdateID): fileUpdateID=%d", 
						fileUpdateID));
		
		logger.info(String.format("Query for BackupFileDataPacket with update ID = %d", fileUpdateID));
		
		BackupFileDataPacket retVal;
		List<BackupFileDataPacket> packetList = repository.findByFileUpdateID(fileUpdateID);
		
		if( 0 == packetList.size() ) {
			retVal = null;
		} else if( 1 == packetList.size() ) {
			retVal = packetList.get(0);
		} else { //packetList.size() > 1
			throw new Exception(String.format("Invalid count returned for fileUpdateID=%d, expected: 1, received: %d", 
												fileUpdateID, packetList.size()));
		}
		
		logger.info(String.format("Data packet %sfound for ID = %d",((retVal == null) ? "not " : ""), fileUpdateID));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getPacketByFileUpdateID(Long fileUpdateID): fileUpdateID=%d with return: %s",
									fileUpdateID, retVal));
		
		return retVal;
	}
	
	@Override
	public BackupFileDataPacket addBackupFileDataPacket(BackupFileDataPacket dataPacket) throws Exception {
		
		if( dataPacket == null ) {
			logger.error("Null BackupFileDataPacket passed as argument to BackupFileDataPacketServiceImpl.addBackupFileDataPacket");
			
			throw new NullPointerException("Backup file data packet can't b null");
		}
		
		BackupFileDataPacket retVal = repository.save(dataPacket);
		
		logger.info("Data packet saved to repository");
		
		return retVal;
		
	}
	
	@Override
	public List<BackupFileDataPacket> getPacketsCreatedAfter(Date createDateTime) throws Exception {

		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getPacketsCreatedAfter(Date createDateTime): dateTime=%s", 
				createDateTime));
		
		if( createDateTime == null ) {
			logger.error("Null create date passed as argument to BackupFileDataPacketServiceImpl.getPacketsCreatedAfter");
			
			throw new NullPointerException("Backup file data packet can't b null");
		}		

		List<BackupFileDataPacket> retVal = null;
		
		logger.info(String.format("Query for BackupFileDataPacket with create date > = %s", createDateTime));

		retVal = repository.findAfterCaptureDateTime(createDateTime);
				
		logger.info(String.format("%d data packets found with create date > %s", retVal.size(), createDateTime));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getPacketsCreatedAfter(Date createDateTime): dateTime=%s with return: %s",
									createDateTime, retVal));
		
		return retVal;
	}
	
}
