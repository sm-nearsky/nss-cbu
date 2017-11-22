package com.nearskysolutions.cloudbackup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
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
public class BackupFileDataServiceTest {

	Logger logger = LoggerFactory.getLogger(CloudBackupClientTestConfig.class);
	
	@Autowired	
	private BackupFileDataService fileDataSvc;
			
	@Autowired	
	private BackupFileClientService clientSvc;
	
	@Test
	public void queryPacketByID() {
		
				
		BackupFileDataPacket dataPacket = new BackupFileDataPacket(1L, 1L, 10, 1, 10, "/foo/bar", "file1.txt", FileAction.Create);
		
		try {
			dataPacket = fileDataSvc.addBackupFileDataPacket(dataPacket);
		
			Long packetID = dataPacket.getDataPacketID();
			
			BackupFileDataPacket packet = fileDataSvc.getPacketByFileDataPacketID(packetID);
			
			assertNotNull(packet);
			assertEquals(packetID, packet.getDataPacketID());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}
	
		
	}
	
	@Test
	public void queryBatchByID() {
						
		UUID clientID = UUID.randomUUID();
	
		BackupFileDataBatch dataBatch = new BackupFileDataBatch(clientID);
		
		try {
			dataBatch = fileDataSvc.addBackupFileDataBatch(dataBatch);
		
			Long batchID = dataBatch.getFileBatchID();
			
			dataBatch = fileDataSvc.getDataBatchByBatchID(batchID);
			
			assertNotNull(dataBatch);
			assertEquals(batchID, dataBatch.getFileBatchID());								
							
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}
		
	}
	
	@Test
	public void queryBatchByDate() {
		
		UUID clientID = UUID.randomUUID();
				
		BackupFileDataBatch dataBatch1 = new BackupFileDataBatch(clientID);
		BackupFileDataBatch dataBatch2 = new BackupFileDataBatch(clientID);
		BackupFileDataBatch dataBatch3 = new BackupFileDataBatch(clientID);
		
				
		try {
			dataBatch1 = fileDataSvc.addBackupFileDataBatch(dataBatch1);
						
			Thread.sleep(1000);
			
			dataBatch2 = fileDataSvc.addBackupFileDataBatch(dataBatch2);
						
			List<BackupFileDataBatch> batchList1 = fileDataSvc.getBatchesCreatedAfter(dataBatch1.getDateTimeCaptured());
						
			assertNotNull(batchList1);
			assertEquals(1, batchList1.size());
			
			assertTrue(batchList1.get(0).getDateTimeCaptured().compareTo(dataBatch1.getDateTimeCaptured()) > 0);
								  
	        for (BackupFileDataBatch batch : batchList1) {
				assertTrue(batch.getDateTimeCaptured().compareTo(dataBatch1.getDateTimeCaptured()) > 0);
			}
	        	                
	        dataBatch3.setDateTimeConfirmed(new Date());	        
	        dataBatch3 = fileDataSvc.addBackupFileDataBatch(dataBatch3);
	        
	        List<BackupFileDataBatch> batchList2 = fileDataSvc.getBatchesPendingConfirm();
			
			assertNotNull(batchList2);
			assertEquals(2, batchList2.size());
											  
	        for (BackupFileDataBatch batch : batchList2) {
	        	assertNull(batch.getDateTimeConfirmed());
			}
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}
		
	}	
	
	@Test
	public void queryBackupFileTrackers() throws Exception {
		
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
									
			bft3.setFileDeleted(true);
			
			fileDataSvc.addBackupFileTracker(bft1);
			fileDataSvc.addBackupFileTracker(bft2);
			fileDataSvc.addBackupFileTracker(bft3);
					
			List<BackupFileTracker> backupTrackers = fileDataSvc.getAllBackupTrackersForClient(clientID);
			
			assertNotNull(backupTrackers);
			assertEquals(3, backupTrackers.size());
			
			backupTrackers = fileDataSvc.getActiveBackupTrackersForClient(clientID);
			
			assertEquals(2, backupTrackers.size());
			assertFalse(backupTrackers.get(0).isFileDeleted());
			assertFalse(backupTrackers.get(1).isFileDeleted());
			
			assertTrue(null != backupTrackers.get(0).getFileAttributes());
			assertTrue(file1Size.longValue() == backupTrackers.get(0).getFileAttributes().getFileSize().longValue());
			
			assertTrue(null != backupTrackers.get(1).getFileAttributes());
			assertTrue(file2Size.longValue() == backupTrackers.get(1).getFileAttributes().getFileSize().longValue());
			
			BackupFileTracker tracker = fileDataSvc.getTrackerByBackupFileTrackerID(backupTrackers.get(0).getBackupFileTrackerID());
			
			assertTrue(null != tracker);
			assertEquals(tracker.getBackupFileTrackerID(), backupTrackers.get(0).getBackupFileTrackerID());
			
		} catch (Exception e) {		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		} finally {
			file1.delete();
			file2.delete();			
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
			backupFileTracker.setFileDeleted(true);
			
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
			assertTrue(backupFileTracker.isFileDeleted());
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
