package com.nearskysolutions.cloudbackup.services;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.data.BackupFileTrackerRepository;

@Component
public class BackupFileDataServiceImpl implements BackupFileDataService {
	
	Logger logger = LoggerFactory.getLogger(BackupFileDataServiceImpl.class);
		
	@Autowired
	private BackupFileTrackerRepository trackerRepo;
	
	@Override
	public BackupFileTracker addBackupFileTracker(BackupFileTracker fileTracker) throws Exception {
		if( fileTracker == null ) {
			logger.error("Null BackupFileTracker passed as argument to BackupFileDataPacketServiceImpl.addBackupFileTracker");
			
			throw new NullPointerException("Backup file tracker data can't be null");
		}
		if( fileTracker.getClientID() == null ) {
			logger.error("Null ClientID passed within BackupFileTracker to BackupFileDataPacketServiceImpl.addBackupFileTracker");
			
			throw new NullPointerException("Client ID within backup file tracker data can't be null");
		}
				
		List<BackupFileTracker> otherTrackers = trackerRepo.findMatchingTrackers(
																fileTracker.getClientID(),
																fileTracker.getBackupRepositoryType(),
																fileTracker.getBackupRepositoryLocation(),																
																fileTracker.getBackupRepositoryKey(),
																fileTracker.getSourceDirectory(),
																fileTracker.getFileName());
				
		if( null != otherTrackers && otherTrackers.size() > 0) {
			if( otherTrackers.size() == 1 && otherTrackers.get(0).isFileDeleted()) {
				//Replacing deleted file
				trackerRepo.delete(otherTrackers.get(0));
			} else {			
				throw new Exception(String.format("Unable to create tracker for existing file: %s", fileTracker.toString()));
			}
		}
			
		fileTracker.setTrackerStatus(BackupFileTrackerStatus.Pending);
		fileTracker.setLastStatusChange(new Date());
		
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
		
		if(fileTracker.getBackupFileTrackerID() == null) {
			logger.error("Null or invalid BackupFileTracker passed as argument to BackupFileDataPacketServiceImpl.updateBackupFileTracker");
			
			throw new NullPointerException("Backup tracker ID can't be null");
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
	public void deleteBackupFileTracker(BackupFileTracker fileTracker) throws Exception {
		
		if( fileTracker == null ) {
			logger.error("Null BackupFileTracker passed as argument to BackupFileDataPacketServiceImpl.deleteBackupFileTracker");
			
			throw new NullPointerException("Backup file tracker data can't be null");
		}
		
		if(fileTracker.getBackupFileTrackerID() == null ) {
			logger.error("Null or invalid BackupFileTracker passed as argument to BackupFileDataPacketServiceImpl.deleteBackupFileTracker");
			
			throw new NullPointerException("Backup tracker ID can't be null");
		}
		
		if( false == trackerRepo.exists(fileTracker.getBackupFileTrackerID())) {
			logger.error("Unknown BackupFileTrackerID (%d) passed to to BackupFileDataPacketServiceImpl.deleteBackupFileTracker", 
							fileTracker.getBackupFileTrackerID());
			
			throw new Exception("Backup file tracker ID not found");
		}		
		
		trackerRepo.delete(fileTracker);
		
		logger.info("Backup file tracker data deleted from repository");	
		
	}

	@Override
	public List<BackupFileTracker> getAllBackupTrackersForClient(UUID clientID) {
		
		logger.trace("In BackupFileDataPacketServiceImpl.getAllBackupTrackersForClientByPage(UUID clientID)");

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileDataPacketServiceImpl.getAllBackupTrackersForClient");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		logger.info(String.format("Query for all BackupFileTracker instances for client = %s", clientID));
				
		List<BackupFileTracker> retVal = trackerRepo.findByClientIDOrderByFileFullPath(clientID);
					
		logger.info(String.format("Query found %d backup file trackers",retVal.size()));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getAllBackupTrackersForClient(): file tracker list size=%d", retVal.size()));
		
		return retVal;
	}

	
	@Override
	public List<BackupFileTracker> getAllBackupTrackersForClientByPage(UUID clientID, int pageID, int pageSize) {
		
		logger.trace("In BackupFileDataPacketServiceImpl.getAllBackupTrackersForClientByPage(UUID clientID, int pageID, int pageSize)");

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileDataPacketServiceImpl.getAllBackupTrackersForClientByPage");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		logger.info(String.format("Query for all BackupFileTracker instances for clientID = %s, pageID=%d, size=%d", 
				clientID.toString(), pageID, pageSize));		
				
		Pageable page = new PageRequest(pageID, pageSize);
		
		List<BackupFileTracker> retVal = trackerRepo.findByClientIDOrderByFileFullPath(clientID, page).getContent();
					
		logger.info(String.format("Query found %d backup file trackers",retVal.size()));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getAllBackupTrackersForClientByPage(UUID clientID, int pageID, int pageSize): file tracker list size=%d", 
					retVal.size()));
		
		return retVal;
	}
	
	@Override
	public List<BackupFileTracker> getActiveBackupTrackersForClient(UUID clientID) {
		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClient(UUID client) : %s", clientID));

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClient");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		logger.info(String.format("Query for all BackupFileTracker instances for client = %s", clientID));
		
		List<BackupFileTracker> retVal = trackerRepo.findByClientIDAndIsFileDeletedFalse(clientID);
					
		logger.info(String.format("Query found %d backup file trackers",retVal.size()));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClient(): file tracker list size=%d", retVal.size()));
		
		return retVal;
	}
	
	@Override
	public List<BackupFileTracker> getActiveBackupTrackersForClientByPage(UUID clientID, int pageID, int pageSize) {
		logger.trace("In BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClientByPage(UUID client, int pageID, int pageSize)");

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClientByPage");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		logger.info(String.format("Query for all BackupFileTracker instances for client = %s, pageID=%d, pageSize=%d", 
						clientID, pageID, pageSize));
		
		Pageable page = new PageRequest(pageID, pageSize);
		
		List<BackupFileTracker> retVal = trackerRepo.findByClientIDAndIsFileDeletedFalse(clientID, page).getContent();
					
		logger.info(String.format("Query found %d backup file trackers",retVal.size()));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getActiveBackupTrackersForClientByPage(): file tracker list size=%d", retVal.size()));
		
		return retVal;
	}

	@Override
	public BackupFileTracker getTrackerByBackupFileTrackerID(UUID trackerID) {
		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getTrackerByBackupFileTrackerID(Long trackerID) : %s", trackerID));

		if( trackerID == null ) {
			logger.error("Null trackerID passed as argument to BackupFileDataPacketServiceImpl.getTrackerByBackupFileTrackerID");
			
			throw new NullPointerException("Tracker ID can't be null");
		}
		
		logger.info(String.format("Query for BackupFileTracker instance with tracker ID: %s", trackerID.toString()));
		
		BackupFileTracker retVal = trackerRepo.findOne(trackerID);
					
		if( null == retVal ) {
			logger.info(String.format("No backup file tracker with ID: %s", trackerID.toString()));
		} else {
			logger.info(String.format("Found backup file tracker with ID: %s", trackerID.toString()));
		}
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getTrackerByBackupFileTrackerID(): file tracker found: %s",
									((retVal != null) ? retVal.toString() : "null")));
		
		return retVal;
	}

	@Override
	public List<BackupFileTracker> findMatchingTrackers(UUID clientID, 
														String backupRepositoryType,
														String backupRepositoryLocation, 
														String backupRepositoryKey, 
														String sourceDirectory, 
														String fileName) {
		
		logger.trace("In BackupFileDataPacketServiceImpl.findMatchingTrackers(UUID clientIDUUID clientID, "+
						"String backupRepositoryType,String backupRepositoryLocation,String backupRepositoryKey, "+ 
						"String sourceDirectory, String fileName)");

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileDataPacketServiceImpl.findMatchingTrackers");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		if( sourceDirectory == null ) {
			logger.error("Null Source directory passed as argument to BackupFileDataPacketServiceImpl.findMatchingTrackers");
			
			throw new NullPointerException("Source directory can't be null");
		}
		
		if( fileName == null ) {
			logger.error("Null file name passed as argument to BackupFileDataPacketServiceImpl.findMatchingTrackers");
			
			throw new NullPointerException("File name can't be null");
		}
		
		logger.info(String.format("Query for all BackupFileTracker instances for clientID=%s, "+
									"backupRepositoryType=%s, backupRepositoryLocation=%s, backupRepositoryKey=%s, "+ 
									"sourceDirectory=%s, fileName=%s", 
									clientID, backupRepositoryType, backupRepositoryLocation, backupRepositoryKey,
									sourceDirectory, fileName));
				
		List<BackupFileTracker> retVal = trackerRepo.findMatchingTrackers(clientID, backupRepositoryType, backupRepositoryLocation, backupRepositoryKey, sourceDirectory, fileName);				
					
		logger.info(String.format("Query found %d backup file trackers", retVal.size()));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.findMatchingTrackers(UUID clientIDUUID clientID, "+
				"String backupRepositoryType,String backupRepositoryLocation,String backupRepositoryKey, "+ 
				"String sourceDirectory, String fileName): file tracker list size=%d", retVal.size()));
		
		return retVal;
	}		
}
