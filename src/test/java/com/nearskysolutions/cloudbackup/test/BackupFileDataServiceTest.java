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
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class BackupFileDataServiceTest {

Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);
	
	@Autowired	
	private BackupFileDataService fileDataSvc;
		
	@Test
	public void TestPacketIDQuery() {
		
				
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
	public void TestBatchIDQuery() {
						
		UUID clientID = UUID.randomUUID();
		Calendar createDateTime = Calendar.getInstance();
				
		List<String> fileList = new ArrayList<String>();
		fileList.add("/foo/bar/file1.txt");
		fileList.add("/fee/boo/file2.txt");
		
		BackupFileDataBatch dataBatch = new BackupFileDataBatch(clientID, createDateTime.getTime(), fileList);
		
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
	public void TestBatchDateQuery() {
		
		UUID clientID = UUID.randomUUID();
		Calendar createDateTime1 = Calendar.getInstance();
		Calendar createDateTime2 = Calendar.getInstance();
		
		List<String> fileList = new ArrayList<String>();
		fileList.add("/foo/bar/file1.txt");
		fileList.add("/fee/boo/file2.txt");
		
		Date compareDateTime = createDateTime1.getTime();
		
		createDateTime2.setTimeInMillis(createDateTime1.getTimeInMillis() + 2000);
		
		BackupFileDataBatch dataBatch1 = new BackupFileDataBatch(clientID, createDateTime1.getTime(), fileList);
		BackupFileDataBatch dataBatch2 = new BackupFileDataBatch(clientID, createDateTime2.getTime(), fileList);
		
				
		try {
			fileDataSvc.addBackupFileDataBatch(dataBatch1);
			fileDataSvc.addBackupFileDataBatch(dataBatch2);
			
			List<BackupFileDataBatch> batchList = fileDataSvc.getBatchesCreatedAfter(compareDateTime);
			
			assertNotNull(batchList);
			assertTrue(batchList.size() > 0);
			
	        for (BackupFileDataBatch batch : batchList) {
				assertTrue(batch.getDateTimeCaptured().compareTo(compareDateTime) > 0);
			}
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
	
		
	}
	
}
