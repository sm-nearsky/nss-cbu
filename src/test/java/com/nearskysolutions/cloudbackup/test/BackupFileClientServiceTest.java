package com.nearskysolutions.cloudbackup.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class BackupFileClientServiceTest {

Logger logger = LoggerFactory.getLogger(BackupFileClientServiceTest.class);
	
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
			
			backupClient = clientSvc.getBackupClientByClientID(clientID);
			
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
				
		BackupFileClient backupClient1 = new BackupFileClient("Test Client abc", "test 1", "type 1", "loc 1", "key 1", directoryList);
		BackupFileClient backupClient2 = new BackupFileClient("Test Client xyz", "test 2", "type 2", "loc 2", "key 2", directoryList);
						
		try {
			
			clientSvc.addBackupClient(backupClient2); //Reverse order to force sort
			
			Thread.sleep(250);
			
			clientSvc.addBackupClient(backupClient1);
			
		
			List<BackupFileClient> clientList = clientSvc.getAllBackupClients();
			
			assertNotNull(clientList);
			assertEquals(2, clientList.size());
			
			assertEquals(backupClient1.getClientName(), clientList.get(0).getClientName());
			assertEquals(backupClient2.getClientName(), clientList.get(1).getClientName());
			
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
			
			backupClient = clientSvc.getBackupClientByClientID(clientID);
			
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
	
	@Test
	public void generateValidationExceptinos() {
						
		List<String> directoryList = new ArrayList<String>();
		directoryList.add("/C/Files");
		directoryList.add("/D/MoreFiles");
		
		BackupFileClient backupClient = new BackupFileClient(null, "test client", "test type", "test loc", "test key", directoryList);
						
		try {		
			try {
				clientSvc.addBackupClient(backupClient);
		        fail("Expected an validation exception to be thrown");
		    } catch (Exception ex) {
		        assertThat(ex.getMessage(), containsString("client name can't be null or empty"));
		    }
			
			backupClient.setClientName(" ");
			
			try {
				clientSvc.addBackupClient(backupClient);
		        fail("Expected an validation exception to be thrown");
		    } catch (Exception ex) {
		        assertThat(ex.getMessage(), containsString("client name can't be null or empty"));
		    }
			
			backupClient.setClientName("test client");
			
			backupClient = clientSvc.addBackupClient(backupClient);
			
			backupClient.setClientName(null);
			
			try {
				clientSvc.updateBackupClient(backupClient);
		        fail("Expected an validation exception to be thrown");
		    } catch (Exception ex) {
		        assertThat(ex.getMessage(), containsString("client name can't be null or empty"));
		    }
			
			backupClient.setClientName(" ");
			
			try {
				clientSvc.updateBackupClient(backupClient);
		        fail("Expected an validation exception to be thrown");
		    } catch (Exception ex) {
		        assertThat(ex.getMessage(), containsString("client name can't be null or empty"));
		    }
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
		
	}
	
}
