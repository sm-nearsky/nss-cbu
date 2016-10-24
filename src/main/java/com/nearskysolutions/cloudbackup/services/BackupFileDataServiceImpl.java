package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.data.BackupFileClientRepository;
import com.nearskysolutions.cloudbackup.data.BackupFileDataBatchRepository;
import com.nearskysolutions.cloudbackup.data.BackupFileDataPacketRepository;
import com.nearskysolutions.cloudbackup.data.BackupFileTrackerRepository;

@Component
public class BackupFileDataServiceImpl implements BackupFileDataService {
	
	Logger logger = LoggerFactory.getLogger(BackupFileDataServiceImpl.class);
	
	@Autowired
	private BackupFileDataPacketRepository packetRepo;
	
	@Autowired
	private BackupFileDataBatchRepository batchRepo;
	
	@Autowired
	private BackupFileClientRepository clientRepo;
	
	@Autowired
	private BackupFileTrackerRepository trackerRepo;

	@Override
	public BackupFileDataPacket getPacketByFileUpdateID(Long fileUpdateID) throws Exception {
		
		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getPacketByFileUpdateID(Long fileUpdateID): fileUpdateID=%d", 
						fileUpdateID));
		
		logger.info(String.format("Query for BackupFileDataPacket with update ID = %d", fileUpdateID));
		
		BackupFileDataPacket retVal;
		List<BackupFileDataPacket> packetList = packetRepo.findByFileUpdateID(fileUpdateID);
		
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
			
			throw new NullPointerException("Backup file data packet can't be null");
		}
		
		BackupFileDataPacket retVal = packetRepo.save(dataPacket);
		
		logger.info("Data packet saved to repository");
		
		return retVal;
		
	}
	
	@Override
	public BackupFileDataBatch getDataBatchByBatchID(Long batchUpdateID) throws Exception {
		
		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getDataBatchByBatchID(Long batchUpdateID): batchUpdateID=%d", 
				batchUpdateID));

		logger.info(String.format("Query for BackupFileDataBatch with batch ID = %d", batchUpdateID));
		
		BackupFileDataBatch retVal;
		List<BackupFileDataBatch> batchList = batchRepo.findByFileBatchID(batchUpdateID);
		
		if( 0 == batchList.size() ) {
			retVal = null;
		} else if( 1 == batchList.size() ) {
			retVal = batchList.get(0);
		} else { //batchList.size() > 1
			throw new Exception(String.format("Invalid count returned for batchUpdateID=%d, expected: 1, received: %d", 
									batchUpdateID, batchList.size()));
		}
		
		logger.info(String.format("File batch %sfound for ID = %d",((retVal == null) ? "not " : ""), batchUpdateID));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getDataBatchByBatchID(Long batchUpdateID): batchUpdateID=%d with return: %s",
									batchUpdateID, retVal));
		
		return retVal;
	}
	
	@Override
	public BackupFileDataBatch addBackupFileDataBatch(BackupFileDataBatch fileDataBatch) throws Exception {
		
		if( fileDataBatch == null ) {
			logger.error("Null BackupFileDataBatch passed as argument to BackupFileDataPacketServiceImpl.addBackupFileDataBatch");
			
			throw new NullPointerException("Backup file data batch can't be null");
		}
		
		BackupFileDataBatch retVal = batchRepo.save(fileDataBatch);
		
		logger.info("File data batch saved to repository");
		
		return retVal;
		
	}
	
	@Override
	public List<BackupFileDataBatch> getBatchesCreatedAfter(Date createDateTime) throws Exception {

		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getBatchesCreatedAfter(Date createDateTime): dateTime=%s", 
				createDateTime));
		
		if( createDateTime == null ) {
			logger.error("Null create date passed as argument to BackupFileDataPacketServiceImpl.getBatchesCreatedAfter");
			
			throw new NullPointerException("Create datetime can't be null");
		}		

		List<BackupFileDataBatch> retVal = null;
		
		logger.info(String.format("Query for BackupFileDataBatch with create date > = %s", createDateTime));

		retVal = batchRepo.findAfterCaptureDateTime(createDateTime);
				
		logger.info(String.format("%d data batches found with create date > %s", retVal.size(), createDateTime));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getBatchesCreatedAfter(Date createDateTime): dateTime=%s with return: %s",
									createDateTime, retVal));
		
		return retVal;
	}
	
	@Override
	public BackupFileTracker addBackupFileTracker(BackupFileTracker fileTracker) throws Exception {
		if( fileTracker == null ) {
			logger.error("Null BackupFileTracker passed as argument to BackupFileDataPacketServiceImpl.addBackupFileTracker");
			
			throw new NullPointerException("Backup file tracker data can't be null");
		}
		
		BackupFileTracker retVal = trackerRepo.save(fileTracker);
		
		logger.info("Backup file tracker saved to repository");
		
		return retVal;
	}

	@Override
	public void updateBackupFileTracker(BackupFileTracker fileTracker) throws Exception {
		
		if( fileTracker == null ) {
			logger.error("Null BackupFileTracker passed as argument to BackupFileDataPacketServiceImpl.updateBackupFileTracker");
			
			throw new NullPointerException("Backup file tracker data can't be null");
		}
		
		if(fileTracker.getBackupFileTrackerID() == null || fileTracker.getBackupFileTrackerID() <= 0) {
			logger.error("Null or invalid BackupFileTracker passed as argument to BackupFileDataPacketServiceImpl.updateBackupFileTracker");
			
			throw new NullPointerException("Backup client UUID can't be null");
		}
		
		if( false == trackerRepo.exists(fileTracker.getBackupFileTrackerID())) {
			logger.error("Unknown BackupFileTrackerID (%d) passed to to BackupFileDataPacketServiceImpl.updateBackupFileTracker", 
							fileTracker.getBackupFileTrackerID());
			
			throw new Exception("Backup file tracker ID not found");
		}
		
		trackerRepo.save(fileTracker);
		
		logger.info("Backup file tracker data updated in repository");	
		
	}

	@Override
	public List<BackupFileTracker> getAllBackupTrackersForClient(UUID clientID) {
		
		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getAllBackupTrackersForClient(UUID client)", clientID));

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileDataPacketServiceImpl.getAllBackupTrackersForClient");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		logger.info(String.format("Query for all BackupFileTracker instances for client = %s", clientID));
		
		List<BackupFileTracker> retVal = trackerRepo.findByClientID(clientID);
					
		logger.info(String.format("Query found %d backup file trackers",retVal.size()));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getAllBackupTrackersForClient(): file tracker list size=%d", retVal.size()));
		
		return retVal;
	}

	@Override
	public List<BackupFileTracker> getActiveBackupTrackersForClient(UUID clientID) {
		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClient(UUID client)", clientID));

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClient");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		logger.info(String.format("Query for all BackupFileTracker instances for client = %s", clientID));
		
		List<BackupFileTracker> retVal = trackerRepo.findByClientIDActiveFilesOnly(clientID);
					
		logger.info(String.format("Query found %d backup file trackers",retVal.size()));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClient(): file tracker list size=%d", retVal.size()));
		
		return retVal;
	}	
	
}
