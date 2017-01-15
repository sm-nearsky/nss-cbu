package com.nearskysolutions.cloudbackup.services;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.data.BackupFileClientRepository;

@Component
public class BackupFileClientServiceImpl implements BackupFileClientService {
	
	Logger logger = LoggerFactory.getLogger(BackupFileClientServiceImpl.class);
	
	@Autowired
	private BackupFileClientRepository clientRepo;
	
	@Override
	public BackupFileClient addBackupClient(BackupFileClient backupClient) throws Exception {
		
		if( backupClient == null ) {
			logger.error("Null BackupFileClient passed as argument to BackupFileDataPacketServiceImpl.addBackupClient");
			
			throw new NullPointerException("Backup client data can't be null");
		}
		
		BackupFileClient retVal = clientRepo.save(backupClient);
		
		logger.info("Backup client data saved to repository");
		
		return retVal;
	}

	@Override
	public void updateBackupClient(BackupFileClient backupClient) throws Exception {
		if( backupClient == null ) {
			logger.error("Null BackupFileClient passed as argument to BackupFileDataPacketServiceImpl.updateBackupClient");
			
			throw new NullPointerException("Backup client data can't be null");
		}
		
		if(backupClient.getClientID() == null) {
			logger.error("Null BackupFileClient passed as argument to BackupFileDataPacketServiceImpl.updateBackupClient");
			
			throw new NullPointerException("Backup client UUID can't be null");
		}
		
		if( false == clientRepo.exists(backupClient.getClientID())) {
			logger.error("Unknown client ID (%s) passed to to BackupFileDataPacketServiceImpl.updateBackupClient", backupClient.getClientID());
			
			throw new Exception("Backup client UUID not found");
		}
		
		clientRepo.save(backupClient);
		
		logger.info("Backup client data updated in repository");	
	
		
	}
	
	@Override
	public BackupFileClient getBackupClientByUUID(UUID clientID) throws Exception {
				
		logger.trace(String.format("In BackupFileDataPacketServiceImpl.getBackupClientByUUID(UUID clientID): clientID=%s", 
						clientID));

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileDataPacketServiceImpl.getBackupClientByUUID");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		logger.info(String.format("Query for BackupFileClient with client ID = %s", clientID));
		
		BackupFileClient retVal;
		List<BackupFileClient> clientList = clientRepo.findByClientID(clientID);
		
		if( 0 == clientList.size() ) {
			retVal = null;
		} else if( 1 == clientList.size() ) {
			retVal = clientList.get(0);
		} else { //clientList.size() > 1
			throw new Exception(String.format("Invalid count returned for clientID=%s, expected: 1, received: %d", 
												clientID, clientList.size()));
		}
		
		logger.info(String.format("Backup client %sfound for ID = %s",((retVal == null) ? "not " : ""), clientID));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getBackupClientByUUID(UUID clientID): clientID=%s with return: %s",
									clientID, retVal));
		
		return retVal;
	}

	@Override
	public List<BackupFileClient> getAllBackupClients() {
		logger.trace("In BackupFileDataPacketServiceImpl.getAllBackupClients()");

		logger.info(String.format("Query for all BackupFileClient instances"));
		
		List<BackupFileClient> retVal = clientRepo.findAllByOrderByClientNameAsc();
					
		logger.info(String.format("Query found %d backup clients",retVal.size()));
		
		logger.trace(String.format("Completed BackupFileDataPacketServiceImpl.getAllBackupClients(): client list size=%d", retVal.size()));
									
		
		return retVal;
	}	
}
