package com.nearskysolutions.cloudbackup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nearskysolutions.cloudbackup.client.CloudBackupClient;
import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class BackupFileClientServiceTest {

Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);
	
	@Autowired	
	private BackupFileClientService clientSvc;
				
	@Test
	public void queryBackupClientByID() {
						
		List<String> directoryList = new ArrayList<String>();
		directoryList.add("/C/Files");
		directoryList.add("/D/MoreFiles");
		
		BackupFileClient backupClient = new BackupFileClient("test client", "test client", "test type", "test loc", "test key", directoryList);
						
		try {
			
			backupClient = clientSvc.addBackupClient(backupClient);
		
			UUID clientID = backupClient.getClientID();
			
			backupClient = clientSvc.getBackupClientByUUID(clientID);
			
			assertNotNull(backupClient);
			assertEquals(clientID, backupClient.getClientID());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void queryAllBackupClientsOrderByName() {
						
		List<String> directoryList = new ArrayList<String>();
		directoryList.add("/C/Files");
		directoryList.add("/D/MoreFiles");
		
		BackupFileClient backupClient1 = new BackupFileClient("abc", "test 1", "type 1", "loc 1", "key 1", directoryList);
		BackupFileClient backupClient2 = new BackupFileClient("xyz", "test 2", "type 2", "loc 2", "key 2", directoryList);
						
		try {
			
			clientSvc.addBackupClient(backupClient2); //Reverse order to force sort
			clientSvc.addBackupClient(backupClient1);
			
		
			List<BackupFileClient> clientList = clientSvc.getAllBackupClients();
			
			assertNotNull(clientList);
			assertEquals(2, clientList.size());
			assertEquals(backupClient1.getClientName(), clientList.get(0).getClientName());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}		
	}
	
	@Test
	public void updateBackupClient() {
					
		List<String> directoryList = new ArrayList<String>();
		directoryList.add("/C/Files");
		directoryList.add("/D/MoreFiles");
		
		BackupFileClient backupClient = new BackupFileClient("test client", "test client", "test type", "test loc", "test key", directoryList);
						
		try {
			
			backupClient = clientSvc.addBackupClient(backupClient);
		
			UUID clientID = backupClient.getClientID();
			
			backupClient.setClientName("updated client");
			backupClient.setClientDescription("updated description");
			backupClient.setCurrentRepositoryType("updated type");
			backupClient.setCurrentRepositoryLocation("updated loc");
			backupClient.setCurrentRepositoryKey("updated key");
			backupClient.getDirectoryIncludes().set(0, "/C/ChangeDir");
			
			clientSvc.updateBackupClient(backupClient);
			
			backupClient = clientSvc.getBackupClientByUUID(clientID);
			
			assertNotNull(backupClient);
			assertEquals("updated client", backupClient.getClientName());
			assertEquals("updated description", backupClient.getClientDescription());
			assertEquals("updated type", backupClient.getCurrentRepositoryType());
			assertEquals("updated loc", backupClient.getCurrentRepositoryLocation());
			assertEquals("updated key", backupClient.getCurrentRepositoryKey());
			assertEquals("/C/ChangeDir", backupClient.getDirectoryIncludes().get(0));
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
		
	}	
	
}
