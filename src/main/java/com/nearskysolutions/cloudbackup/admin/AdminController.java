package com.nearskysolutions.cloudbackup.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.BackupRestoreRequestService;
import com.nearskysolutions.cloudbackup.services.FileHandlerService;;

@RestController
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class AdminController {
	
	@Autowired	
	private BackupFileClientService clientSvc;
	
	@Autowired 
	private BackupRestoreRequestService restoreSvc;
	
	@Autowired 
	private BackupFileDataService backupDataSvc;
	
	@Autowired 
	private FileHandlerService fileSvc;
	
	Logger logger = LoggerFactory.getLogger(AdminController.class);
	
	@RequestMapping(value="/clients", method=RequestMethod.GET)
    public ResponseEntity<List<BackupFileClient>> getClientList() {
		
		logger.trace("In AdminController.getClientList()");

		logger.info("Admin controller called to retrieve full client list");
		
		List<BackupFileClient> clientList = clientSvc.getAllBackupClients();

		logger.info(String.format("Returning client list with %d entries", clientList.size()));
		
		logger.trace("Completed AdminController.getClientList()");
		
		return new ResponseEntity<List<BackupFileClient>>(clientList, HttpStatus.OK);		
    }
	
	@RequestMapping(value="/clients/{clientID}", method=RequestMethod.GET)
    public ResponseEntity<BackupFileClient> getClientByID(@PathVariable UUID clientID) {
		
		logger.trace(String.format("In AdminController.getClientByID(UUID clientID) : %s", clientID));
		
		logger.info(String.format("Admin controller called to retrieve backup client by clientID: %s", clientID));
		
		ResponseEntity<BackupFileClient> retVal = null;
		
		try
		{			
			HttpStatus httpStatus;
			BackupFileClient client = clientSvc.getBackupClientByClientID(clientID);
			
			if( null == client ) {
				httpStatus = HttpStatus.NOT_FOUND;
				
				logger.info(String.format("Backup client not found for clientID: %s", clientID));
			} else {
				httpStatus = HttpStatus.OK;
				
				logger.info(String.format("Backup client found for clientID: %s", clientID));
			}
				
			retVal = new ResponseEntity<BackupFileClient>(client, httpStatus);
		
		} catch(Exception ex) {
			logger.error("Error in AdminController.getClientByID", ex);
			
			retVal = new ResponseEntity<BackupFileClient>((BackupFileClient)null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		logger.trace(String.format("Completed AdminController.getClientByID(UUID clientID) : %s = %s", clientID, retVal.getBody()));
		
		return retVal;
    }
	
	@RequestMapping(value="/clients", method=RequestMethod.PUT)
    public ResponseEntity<Void> upsertBackupClient(@RequestBody BackupFileClient backupClient) {
		
		logger.trace("In AdminController.upsertBackupClient(BackupFileClient backupClient)");
		
		ResponseEntity<Void> retVal;
		
		try 
		{
			HttpStatus httpStatus;
			
			if( null == backupClient.getClientID() ) {				
				logger.info("Admin controller called to add backup client");
				
				clientSvc.addBackupClient(backupClient);				
				httpStatus = HttpStatus.CREATED;
			} else {				
				logger.info(String.format("Admin controller updating backup client with clientID: %s", backupClient.getClientID()));
				
				clientSvc.updateBackupClient(backupClient);
				httpStatus = HttpStatus.OK;
			}
			
			retVal = new ResponseEntity<Void>(httpStatus);
						
		} catch(Exception ex) {
			logger.error("Error in AdminController.upsertBackupClient", ex);
			
			retVal = new ResponseEntity<Void>((Void)null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		logger.trace("Completed AdminController.upsertBackupClient(BackupFileClient backupClient)");
		
		return retVal;
    }
	
	@RequestMapping(value="/clients/{clientID}", method=RequestMethod.DELETE)
    public ResponseEntity<Void> deleteBackupClient(@PathVariable UUID clientID) {
		
		logger.trace(String.format("In AdminController.deleteBackupClient(UUID clientID) : %s", clientID));
		
		logger.info(String.format("Admin controller called to delete backup client with clientID: %s", clientID));
		
		ResponseEntity<Void> retVal;
		
		try 
		{
			HttpStatus httpStatus;
			
			if( null == clientID ) {
				throw new Exception("Client ID can't be null");
			} else {
				BackupFileClient clientToDelete = clientSvc.getBackupClientByClientID(clientID);
				
				if(null == clientToDelete) {
					httpStatus = HttpStatus.NOT_FOUND;
				} else {
					clientSvc.deleteBackupClient(clientID);
					httpStatus = HttpStatus.OK;
				}
			}
			
			retVal = new ResponseEntity<Void>(httpStatus);
						
		} catch(Exception ex) {			
			logger.error("Error in AdminController.deleteBackupClient", ex);
			
			retVal = new ResponseEntity<Void>((Void)null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		logger.trace(String.format("Completed AdminController.deleteBackupClient(UUID clientID) : %s", clientID));
		
		return retVal;
    }
	
	@RequestMapping(value="/restoreRequests", method=RequestMethod.PUT)
    public ResponseEntity<BackupRestoreRequest> insertBackupRestoreRequest(@RequestBody BackupRestoreRequest restoreRequest) {
		
		logger.trace("In AdminController.insertBackupRestoreRequest(BackupRestoreRequest restoreRequest)");
		
		ResponseEntity<BackupRestoreRequest> retVal;
		
		try 
		{
			HttpStatus httpStatus;
			
			if( null != restoreRequest.getRequestID() ) {				
				throw new Exception("Can't add restore request with an existing request ID");
			} else {
				logger.info("Admin controller called to add backup restore request");
				
				restoreSvc.addRestoreRequest(restoreRequest);				
				httpStatus = HttpStatus.CREATED;				
			}
			
			retVal = new ResponseEntity<BackupRestoreRequest>(httpStatus);
						
		} catch(Exception ex) {
			logger.error("Error in AdminController.insertBackupRestoreRequest", ex);
			
			retVal = new ResponseEntity<BackupRestoreRequest>((BackupRestoreRequest)null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		logger.trace("Completed AdminController.insertBackupRestoreRequest(BackupRestoreRequest restoreRequest)");
		
		return retVal;
    }
	
	@RequestMapping(value="/restoreRequests", method=RequestMethod.GET)
    public ResponseEntity<List<BackupRestoreRequest>> getRestoreRequests() {
		
		logger.trace("In AdminController.getRestoreRequests()");

		logger.info("Admin controller called to retrieve full restore request list");
		
		List<BackupRestoreRequest> requestList = restoreSvc.getAllRestoreRequests();

		logger.info(String.format("Returning request list list with %d entries", requestList.size()));
		
		logger.trace("Completed AdminController.getRestoreRequests()");
		
		return new ResponseEntity<List<BackupRestoreRequest>>(requestList, HttpStatus.OK);		
    }
	
	@RequestMapping(value="/restoreRequests/{restoreRequestID}", method=RequestMethod.GET)
    public ResponseEntity<BackupRestoreRequest> getReqstoreRequestByID(@PathVariable UUID restoreRequestID) {
		
		logger.trace(String.format("In AdminController.getReqstoreRequestByID(UUID restoreRequestID) : %s", restoreRequestID));
		
		logger.info(String.format("Admin controller called to retrieve backup restore request by restoreRequestID: %s", restoreRequestID));
		
		ResponseEntity<BackupRestoreRequest> retVal = null;
		
		try
		{			
			HttpStatus httpStatus;
			BackupRestoreRequest restoreRequest = restoreSvc.getRestoreRequestByRequestID(restoreRequestID);
			
			if( null == restoreRequest ) {
				httpStatus = HttpStatus.NOT_FOUND;
				
				logger.info(String.format("Restore request not found for restoreRequestID: %s", restoreRequestID));
			} else {
				httpStatus = HttpStatus.OK;
				
				logger.info(String.format("Restore request found for restoreRequestID: %s", restoreRequestID));
			}
				
			retVal = new ResponseEntity<BackupRestoreRequest>(restoreRequest, httpStatus);
		
		} catch(Exception ex) {
			logger.error("Error in AdminController.getReqstoreRequestByID", ex);
			
			retVal = new ResponseEntity<BackupRestoreRequest>((BackupRestoreRequest)null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		logger.trace(String.format("Completed AdminController.getRestoreRequestByRequestID(UUID restoreRequestID) : %s = %s", restoreRequestID, retVal.getBody()));
		
		return retVal;
    }
	
	@RequestMapping(value="/restoreRequests/{restoreRequestID}", method=RequestMethod.DELETE)
    public ResponseEntity<Void> cancelRestoreRequest(@PathVariable UUID restoreRequestID) {
		
		logger.trace(String.format("In AdminController.cancelRestoreRequest(UUID restoreRequestID) : %s", restoreRequestID));
		
		logger.info(String.format("Admin controller called to cancel backup restore request by restoreRequestID: %s", restoreRequestID));
		
		
		ResponseEntity<Void> retVal;
		
		try 
		{
			HttpStatus httpStatus;
			
			if( null == restoreRequestID ) {
				throw new Exception("Restore request ID can't be null");
			} else {
				BackupRestoreRequest restoreRequest = restoreSvc.getRestoreRequestByRequestID(restoreRequestID);
				
				if(null == restoreRequest) {
					httpStatus = HttpStatus.NOT_FOUND;
				} else {
					restoreSvc.cancelRestoreRequestForID(restoreRequestID);
					httpStatus = HttpStatus.OK;
				}
			}
			
			retVal = new ResponseEntity<Void>(httpStatus);
						
		} catch(Exception ex) {			
			logger.error("Error in AdminController.cancelRestoreRequest", ex);
			
			retVal = new ResponseEntity<Void>((Void)null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		logger.trace(String.format("Completed AdminController.cancelRestoreRequest(UUID restoreRequestID) : %s", restoreRequestID));
		
		return retVal;
    }
	
	//For testing only, won't work on server
	@RequestMapping(value="/doFullBackup/{clientID}", method=RequestMethod.POST)
    public ResponseEntity<Void> doFullBackup(@PathVariable UUID clientID) {
		
		logger.trace("In AdminController.doFullBackup()");
		
		ResponseEntity<Void> retVal;
		
		try {
									
			List<BackupFileTracker> backupTrackers = backupDataSvc.getAllBackupTrackersForClient(clientID);
			BackupFileDataBatch fileBatch = this.backupDataSvc.addBackupFileDataBatch(new BackupFileDataBatch(clientID));
			
			for(BackupFileTracker tracker : backupTrackers) {								
				this.fileSvc.createPacketsForFile(fileBatch, tracker);
			}	
			
			this.fileSvc.sendBatchToProcessingQueue(fileBatch);
			
			retVal = new ResponseEntity<Void>((Void)null, HttpStatus.OK);
		
		} catch (Exception ex) {
			logger.error("Error in AdminController.doFullBackup", ex);
			
			retVal = new ResponseEntity<Void>((Void)null, HttpStatus.INTERNAL_SERVER_ERROR);
		}		
		
		logger.trace("Completed AdminController.doFullBackup()");
		
		return retVal;		
    }
}
