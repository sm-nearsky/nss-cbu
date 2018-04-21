package com.nearskysolutions.cloudbackup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Calendar;
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

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class BackupFileDataServiceTest {

	Logger logger = LoggerFactory.getLogger(CloudBackupClientTestConfig.class);
	
	@Autowired	
	private BackupFileDataService fileDataSvc;
			
	@Autowired	
	private BackupFileClientService clientSvc;
		
	@Test
	public void queryBackupFileTrackers() throws Exception {
		
		UUID clientID = UUID.randomUUID();
		String fileName1 = String.format("1_%s",UUID.randomUUID().toString());
		String fileName2 = String.format("2_%s",UUID.randomUUID().toString());
		String fileName3 = String.format("3_%s",UUID.randomUUID().toString());
				
		File file1 = File.createTempFile(fileName1, null);		
		File file2 = File.createTempFile(fileName2, null);
		File file3 = File.createTempFile(fileName3, null);
		
		
		try {
			
			BackupFileTracker bft1 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															file1.getAbsolutePath());
			
			Long file1Size = bft1.getFileAttributes().getFileSize();
			
			BackupFileTracker bft2 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															file2.getAbsolutePath());
	
			Long file2Size = bft2.getFileAttributes().getFileSize();
			
			BackupFileTracker bft3 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															file3.getAbsolutePath());
						
			fileDataSvc.addBackupFileTracker(bft1);
			fileDataSvc.addBackupFileTracker(bft2);
			
			BackupFileTracker delTracker = fileDataSvc.addBackupFileTracker(bft3);
			fileDataSvc.deleteBackupFileTracker(delTracker);		
			
			List<BackupFileTracker> backupTrackers = fileDataSvc.getAllBackupTrackersForClient(clientID);
			
			assertNotNull(backupTrackers);
			assertEquals(2, backupTrackers.size());
			
			assertTrue(null != backupTrackers.get(0).getFileAttributes());
			assertTrue(file1Size.longValue() == backupTrackers.get(0).getFileAttributes().getFileSize().longValue());
			
			assertTrue(null != backupTrackers.get(1).getFileAttributes());
			assertTrue(file2Size.longValue() == backupTrackers.get(1).getFileAttributes().getFileSize().longValue());
			
			BackupFileTracker tracker = fileDataSvc.getTrackerByBackupFileTrackerID(backupTrackers.get(0).getBackupFileTrackerID(), clientID);
			
			assertTrue(null != tracker);
			assertEquals(tracker.getBackupFileTrackerID(), backupTrackers.get(0).getBackupFileTrackerID());
			
		} catch (Exception e) {		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		} finally {
			file1.delete();
			file2.delete();
			file3.delete();
		}
	}
	
	@Test
	public void queryBackupFileTrackersWithPaging() throws Exception {
		
		UUID clientID = UUID.randomUUID();
				
		File file1 = File.createTempFile(String.format("a1%s", UUID.randomUUID().toString()),"tmp");		
		File file2 = File.createTempFile(String.format("b2%s", UUID.randomUUID().toString()),"tmp");
		File file3 = File.createTempFile(String.format("c3%s", UUID.randomUUID().toString()),"tmp");
		
		List<BackupFileTracker> backupTrackers;
		
		try {
			
			BackupFileTracker bft1 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															file1.getAbsolutePath());
			
			
			BackupFileTracker bft2 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															file2.getAbsolutePath());
	
						
			BackupFileTracker bft3 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															file3.getAbsolutePath());
			
			bft1 = fileDataSvc.addBackupFileTracker(bft1);
			bft2 = fileDataSvc.addBackupFileTracker(bft2);
			bft3 = fileDataSvc.addBackupFileTracker(bft3);
					
			backupTrackers = fileDataSvc.getAllBackupTrackersForClientByPage(clientID, 0, 10);
			
			assertNotNull(backupTrackers);
			assertEquals(3, backupTrackers.size());
			
			assertEquals(bft1.getBackupFileTrackerID(), backupTrackers.get(0).getBackupFileTrackerID());
			assertEquals(bft2.getBackupFileTrackerID(), backupTrackers.get(1).getBackupFileTrackerID());
			assertEquals(bft3.getBackupFileTrackerID(), backupTrackers.get(2).getBackupFileTrackerID());
			
			backupTrackers = fileDataSvc.getAllBackupTrackersForClientByPage(clientID, 0, 2);
			
			assertNotNull(backupTrackers);
			assertEquals(2, backupTrackers.size());
			
			assertEquals(bft1.getBackupFileTrackerID(), backupTrackers.get(0).getBackupFileTrackerID());
			assertEquals(bft2.getBackupFileTrackerID(), backupTrackers.get(1).getBackupFileTrackerID());
			
			backupTrackers = fileDataSvc.getAllBackupTrackersForClientByPage(clientID, 1, 2);
			
			assertNotNull(backupTrackers);
			assertEquals(1, backupTrackers.size());
			
			assertEquals(bft3.getBackupFileTrackerID(), backupTrackers.get(0).getBackupFileTrackerID());

			backupTrackers = fileDataSvc.getAllBackupTrackersForClientByPage(clientID, 100, 2);
			
			assertNotNull(backupTrackers);
			assertEquals(0, backupTrackers.size());
			
		} catch (Exception e) {		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		} finally {
			file1.delete();
			file2.delete();			
			file3.delete();
		}
	}
	
	@Test
	public void queryForMatchingTracker() throws Exception {
		
		UUID clientID = UUID.randomUUID();
		String fileName1 = UUID.randomUUID().toString();
		String fileName2 = UUID.randomUUID().toString();
				
		File file1 = File.createTempFile(fileName1, null);		
		File file2 = File.createTempFile(fileName2, null);
		File file3 = File.createTempFile(fileName2, null);
		
		
		try {
			
			BackupFileTracker bft1 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															file1.getAbsolutePath());
						
			BackupFileTracker bft2 = new BackupFileTracker(clientID, 
															"Repository Type 2", 
															"Repository Location 2", 
															"Repository Key 2", 
															file2.getAbsolutePath());
				
			BackupFileTracker bft3 = new BackupFileTracker(clientID, 
															"Repository Type 3", 
															"Repository Location 3", 
															"Repository Key 3", 
															file3.getAbsolutePath());
			
			bft1 = fileDataSvc.addBackupFileTracker(bft1);
			bft2 = fileDataSvc.addBackupFileTracker(bft2);
			bft3 = fileDataSvc.addBackupFileTracker(bft3);
					
			List<BackupFileTracker> backupTrackers = fileDataSvc.findMatchingTrackers(clientID, 
																						bft2.getBackupRepositoryType(), 
																						bft2.getBackupRepositoryLocation(), 
																						bft2.getBackupRepositoryKey(), 
																						bft2.getFileFullPath());
			
			assertNotNull(backupTrackers);
			assertEquals(1, backupTrackers.size());
			
			assertEquals(bft2.getBackupFileTrackerID(), backupTrackers.get(0).getBackupFileTrackerID());
			
		} catch (Exception e) {		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		} finally {
			file1.delete();
			file2.delete();
			file3.delete();
		}
	}
	
	@Test
	public void updateBackupFileTrackers() {
		
		File tempFile = null;
		
		try {
			
			tempFile = File.createTempFile(UUID.randomUUID().toString(), null);
			
			BackupFileClient client = new BackupFileClient("client 1", "desc", "type 1", "loc 1", "key 1", null);
			
			client = clientSvc.addBackupClient(client);
			
			BackupFileTracker backupFileTracker = new BackupFileTracker(client.getClientID(), 
																		"Repository Type 1", 
																		"Repository Location 1", 
																		"Repository Key 1", 
																		tempFile.getAbsolutePath());
						
			backupFileTracker = fileDataSvc.addBackupFileTracker(backupFileTracker);
			
			Calendar fileUpdateTimestamp = Calendar.getInstance();
			fileUpdateTimestamp.setTimeInMillis(fileUpdateTimestamp.getTimeInMillis() + 1000);
			
			Calendar fileDeletedTimestamp = Calendar.getInstance();
			fileDeletedTimestamp.setTimeInMillis(fileDeletedTimestamp.getTimeInMillis() + 2000);
			
			backupFileTracker.setBackupRepositoryType("updated type");
			backupFileTracker.setBackupRepositoryLocation("updated loc");
			backupFileTracker.setBackupRepositoryKey("updated key");
			backupFileTracker.setFileName("updated file name");
			backupFileTracker.setSourceDirectory("updated directory");
			backupFileTracker.getFileAttributes().setFileModifiedDateTimeMillis(fileUpdateTimestamp.getTimeInMillis());
						
			backupFileTracker.getFileAttributes().setFileSize(3000L);
			backupFileTracker.getFileAttributes().setIsHidden(true);
			
			fileDataSvc.updateBackupFileTracker(backupFileTracker);
						
			List<BackupFileTracker> fileTrackers = fileDataSvc.getAllBackupTrackersForClient(client.getClientID());
			
			assertNotNull(fileTrackers);
			assertEquals(1, fileTrackers.size());
			
			backupFileTracker = fileTrackers.get(0);
						
			assertEquals("updated type", backupFileTracker.getBackupRepositoryType());
			assertEquals("updated loc", backupFileTracker.getBackupRepositoryLocation());
			assertEquals("updated key", backupFileTracker.getBackupRepositoryKey());
			assertEquals("updated file name", backupFileTracker.getFileName());
			assertEquals("updated directory", backupFileTracker.getSourceDirectory());
			assertTrue(fileUpdateTimestamp.getTime().toInstant().toEpochMilli() == backupFileTracker.getFileAttributes().getFileModifiedDateTimeMillis());			
			assertTrue(3000L == backupFileTracker.getFileAttributes().getFileSize().longValue());
			assertTrue(backupFileTracker.getFileAttributes().isHidden());
			
		} catch (Exception e) {		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		} finally {
			tempFile.delete();
		}
		
	}
	
}
