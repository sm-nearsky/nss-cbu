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
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class BackupFileDataServiceTest {

Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);
	
	@Autowired	
	private BackupFileDataService fileDataSvc;
			
	@Test
	public void queryPacketByID() {
		
				
		BackupFileDataPacket dataPacket = new BackupFileDataPacket(1L, 100, 10, 1, 10, "/foo/bar", "file1.txt", "abcd", FileAction.Create);
		
		try {
			dataPacket = fileDataSvc.addBackupFileDataPacket(dataPacket);
		
			Long updateID = dataPacket.getFileUpdateID();
			
			BackupFileDataPacket packet = fileDataSvc.getPacketByFileUpdateID(updateID);
			
			assertNotNull(packet);
			assertEquals(updateID, packet.getFileUpdateID());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
	
		
	}
	
	@Test
	public void queryBatchByID() {
						
		UUID clientID = UUID.randomUUID();
						
		List<String> fileList = new ArrayList<String>();
		fileList.add("/foo/bar/file1.txt");
		fileList.add("/fee/boo/file2.txt");
		
		BackupFileDataBatch dataBatch = new BackupFileDataBatch(clientID, fileList);
		
		try {
			dataBatch = fileDataSvc.addBackupFileDataBatch(dataBatch);
		
			Long batchID = dataBatch.getFileBatchID();
			
			dataBatch = fileDataSvc.getDataBatchByBatchID(batchID);
			
			assertNotNull(dataBatch);
			assertEquals(batchID, dataBatch.getFileBatchID());
			
			assertEquals(fileList.size(), dataBatch.getFileList().size());
			
			for(int i = 0; i < fileList.size(); i++) {			
				assertEquals(fileList.get(i), dataBatch.getFileList().get(i));
			}
				
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void queryBatchByDate() {
		
		UUID clientID = UUID.randomUUID();
		
		List<String> fileList = new ArrayList<String>();
		fileList.add("/foo/bar/file1.txt");
		fileList.add("/fee/boo/file2.txt");
								
		BackupFileDataBatch dataBatch1 = new BackupFileDataBatch(clientID, fileList);
		BackupFileDataBatch dataBatch2 = new BackupFileDataBatch(clientID, fileList);
		
				
		try {
			dataBatch1 = fileDataSvc.addBackupFileDataBatch(dataBatch1);
			
			Thread.sleep(50);
			
			dataBatch2 = fileDataSvc.addBackupFileDataBatch(dataBatch2);
						
			List<BackupFileDataBatch> batchList = fileDataSvc.getBatchesCreatedAfter(dataBatch1.getDateTimeCaptured());
			
			assertNotNull(batchList);
			assertTrue(batchList.size() > 0);
			
	        for (BackupFileDataBatch batch : batchList) {
				assertTrue(batch.getDateTimeCaptured().compareTo(dataBatch1.getDateTimeCaptured()) > 0);
			}
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
		
	}	
	
	@Test
	public void queryBackupFileTrackers() {
		
		UUID clientID = UUID.randomUUID();
		
		BackupFileTracker bft1 = new BackupFileTracker(clientID, 
														"Repository Type 1", 
														"Repository Location 1", 
														"Repository Key 1", 
														"File Name 1", 
														"Source Directory 1", 
														new Date(), 
														new Date(), 
														null);
		
		BackupFileTracker bft2 = new BackupFileTracker(clientID, 
														"Repository Type 2", 
														"Repository Location 2", 
														"Repository Key 2", 
														"File Name 2", 
														"Source Directory 2", 
														new Date(), 
														new Date(), 
														new Date());
		
		try {
			
			fileDataSvc.addBackupFileTracker(bft1);
			fileDataSvc.addBackupFileTracker(bft2);
			
		
			List<BackupFileTracker> backupTrackers = fileDataSvc.getAllBackupTrackersForClient(clientID);
			
			assertNotNull(backupTrackers);
			assertEquals(2, backupTrackers.size());
			
			backupTrackers = fileDataSvc.getActiveBackupTrackersForClient(clientID);
			
			assertEquals(1, backupTrackers.size());
			assertTrue(null == backupTrackers.get(0).getFileDeletedDateTime());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}		
	}
	
	@Test
	public void updateBackupFileTrackers() {
					
		UUID clientID = UUID.randomUUID();
		
		BackupFileTracker backupFileTracker = new BackupFileTracker(clientID, 
																	"Repository Type 1", 
																	"Repository Location 1", 
																	"Repository Key 1", 
																	"File Name 1", 
																	"Source Directory 1", 
																	new Date(), 
																	new Date(), 
																	null);
						
		try {
			
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
			backupFileTracker.setFileModifiedDateTime(fileUpdateTimestamp.getTime());
			backupFileTracker.setFileDeletedDateTime(fileDeletedTimestamp.getTime());
			
			fileDataSvc.updateBackupFileTracker(backupFileTracker);
						
			List<BackupFileTracker> fileTrackers = fileDataSvc.getActiveBackupTrackersForClient(clientID);
			
			assertNotNull(fileTrackers);
			assertEquals(1, fileTrackers.get(0));
			
			backupFileTracker = fileTrackers.get(0);
						
			assertEquals("updated type", backupFileTracker.getBackupRepositoryType());
			assertEquals("updated loc", backupFileTracker.getBackupRepositoryLocation());
			assertEquals("updated key", backupFileTracker.getBackupRepositoryKey());
			assertEquals("updated file name", backupFileTracker.getFileName());
			assertEquals("updated directory", backupFileTracker.getSourceDirectory());
			assertEquals(fileUpdateTimestamp.getTime(), backupFileTracker.getFileModifiedDateTime());
			assertEquals(fileDeletedTimestamp.getTime(), backupFileTracker.getFileDeletedDateTime());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
		
	}
	
}
