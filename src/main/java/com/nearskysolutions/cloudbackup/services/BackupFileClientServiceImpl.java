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
		
		logger.trace("In BackupFileClientServiceImpl.addBackupClient(BackupFileClient backupClient)");
		
		if( backupClient == null ) {
			logger.error("Null BackupFileClient passed as argument to BackupFileClientServiceImpl.addBackupClient");
			
			throw new NullPointerException("Backup client data can't be null");
		} else if( null != backupClient.getClientID() ) {
			logger.error("BackupFileClient passed with client ID to BackupFileClientServiceImpl.addBackupClient");
			
			throw new Exception("Can't pass client ID on backup client add");
		} else if( null == backupClient.getClientName() || 0 == backupClient.getClientName().trim().length()) {
			logger.error("Null or empty client name passed to BackupFileClientServiceImpl.addBackupClient");
			
			throw new Exception("BackupFileClient client name can't be null or empty");
		}
		
		BackupFileClient retVal = clientRepo.save(backupClient);
		
		logger.info("Backup client data saved to repository");
		
		logger.trace("Completed BackupFileClientServiceImpl.addBackupClient(BackupFileClient backupClient)");
		
		return retVal;
	}

	@Override
	public void updateBackupClient(BackupFileClient backupClient) throws Exception {
		
		logger.trace("In BackupFileClientServiceImpl.updateBackupClient(BackupFileClient backupClient)");
		
		if( backupClient == null ) {
			logger.error("Null BackupFileClient passed as argument to BackupFileClientServiceImpl.updateBackupClient");
			
			throw new NullPointerException("Backup client data can't be null");
		} else 	if(backupClient.getClientID() == null) {
			logger.error("Null BackupFileClient passed as argument to BackupFileClientServiceImpl.updateBackupClient");
			
			throw new NullPointerException("Backup client UUID can't be null");
		} else if( null == this.getBackupClientByClientID(backupClient.getClientID())) {
			logger.error("Unknown client ID (%s) passed to to BackupFileClientServiceImpl.updateBackupClient", backupClient.getClientID());
			
			throw new Exception("Backup client ID not found");
		}else if( null == backupClient.getClientName() || 0 == backupClient.getClientName().trim().length()) {
			logger.error("Null or empty client name passed to BackupFileClientServiceImpl.updateBackupClient");
			
			throw new Exception("BackupFileClient client name can't be null or empty");
		}
		
		clientRepo.save(backupClient);
		
		logger.info("Backup client record updated in repository");
		
		logger.trace("Completed BackupFileClientServiceImpl.updateBackupClient(BackupFileClient backupClient)");
	}
	
	@Override
	public void deleteBackupClient(UUID clientID) throws Exception {
		
		logger.trace(String.format("In BackupFileClientServiceImpl.deleteBackupClient(UUID clientID): clientID=%s", 
				clientID));
		
		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileClientServiceImpl.deleteBackupClient");			
			throw new NullPointerException("Client ID can't be null");
		}
		
		if( null == this.getBackupClientByClientID(clientID)) {
			logger.error("Unknown client ID (%s) passed to to BackupFileClientServiceImpl.deleteBackupClient", clientID.toString());
			
			throw new Exception("Backup client ID not found");
		}
		
		clientRepo.delete(clientID);
		
		logger.info("Backup client record delete from repository");	
	
		logger.trace(String.format("Completed BackupFileClientServiceImpl.deleteBackupClient(UUID clientID): clientID=%s",
					clientID));
	}
	
	@Override
	public BackupFileClient getBackupClientByClientID(UUID clientID) throws Exception {
				
		logger.trace(String.format("In BackupFileClientServiceImpl.getBackupClientByUUID(UUID clientID): clientID=%s", 
						clientID));

		if( clientID == null ) {
			logger.error("Null clientID passed as argument to BackupFileClientServiceImpl.getBackupClientByUUID");
			
			throw new NullPointerException("Client ID can't be null");
		}
		
		logger.info(String.format("Query for BackupFileClient with client ID = %s", clientID));

		List<BackupFileClient> lstClients = clientRepo.findBySingleClientID(clientID.toString());
		
		BackupFileClient retVal = lstClients.size() == 1 ? lstClients.get(0) : null;
		
		logger.info(String.format("Backup client %sfound for ID = %s",((retVal == null) ? "not " : ""), clientID));
		
		logger.trace(String.format("Completed BackupFileClientServiceImpl.getBackupClientByUUID(UUID clientID): clientID=%s with return: %s",
									clientID, retVal));
		
		return retVal;
	}

	@Override
	public List<BackupFileClient> getAllBackupClients() {
		logger.trace("In BackupFileClientServiceImpl.getAllBackupClients()");

		logger.info(String.format("Query for all BackupFileClient instances"));
		
		List<BackupFileClient> retVal = clientRepo.findAllByOrderByClientNameAsc();
					
		logger.info(String.format("Query found %d backup clients",retVal.size()));
		
		logger.trace(String.format("Completed BackupFileClientServiceImpl.getAllBackupClients(): client list size=%d", retVal.size()));
											
		return retVal;
	}	
}
