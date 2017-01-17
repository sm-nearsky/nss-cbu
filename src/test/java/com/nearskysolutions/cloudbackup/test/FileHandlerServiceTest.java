package com.nearskysolutions.cloudbackup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.FilePacketHandlerQueue;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.FileHandlerService;
import com.nearskysolutions.cloudbackup.test.beans.PacketHandlerQueueTest;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class FileHandlerServiceTest {

Logger logger = LoggerFactory.getLogger(FileHandlerServiceTest.class);
	
	@Autowired	
	private FileHandlerService fileHandlerSvc;
				
	@Autowired
	private BackupFileDataService fileDataSvc;
		
	@Autowired
	@Qualifier("PacketHandlerQueue")
	private FilePacketHandlerQueue packetHandlerQueueTest;
		
	@Test
	public void updateTrackerListings() throws IOException {
		
		Path tempDir = null;			
		Path testFile1 = null;
		Path testFile2 = null;
		Path testFile3 = null;
		FileOutputStream fos = null;
		
		UUID clientID = UUID.randomUUID();
	
		try
		{
			tempDir = Files.createTempDirectory(null, new FileAttribute<?>[0]);
			tempDir.toFile().deleteOnExit();
			testFile1 = Files.createTempFile(tempDir, "trackerListingTest1", null, new FileAttribute<?>[0]);
			testFile1.toFile().deleteOnExit();
			testFile2 = Files.createTempFile(tempDir, "trackerListingTest2", null, new FileAttribute<?>[0]);
			testFile2.toFile().deleteOnExit();
				
			BackupFileTracker t1 = new BackupFileTracker(clientID, 
														"Repository Type 1", 
														"Repository Location 1", 
														"Repository Key 1", 
														testFile1.toFile().getAbsolutePath());
			
			BackupFileTracker t2 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															testFile2.toFile().getAbsolutePath());
			
			fileDataSvc.addBackupFileTracker(t1);
			fileDataSvc.addBackupFileTracker(t2);
			
			this.fileHandlerSvc.updateFileTrackerListing(clientID, 
															tempDir.toFile().getAbsolutePath(),
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1");
			
			List<BackupFileTracker> trackerList = this.fileDataSvc.getAllBackupTrackersForClient(clientID);
			
			assertEquals(2, trackerList.size());
			
			testFile3 = Files.createTempFile(tempDir, "trackerListingTest3", null, new FileAttribute<?>[0]);
			testFile3.toFile().deleteOnExit();
			
			//Should pick up new file as tracker
			this.fileHandlerSvc.updateFileTrackerListing(clientID, 
															tempDir.toFile().getAbsolutePath(),
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1");
			
			trackerList = this.fileDataSvc.getAllBackupTrackersForClient(clientID);

			assertEquals(3, trackerList.size());
			
			testFile1.toFile().delete();
			testFile1 = null;
			
			fos = new FileOutputStream(testFile2.toFile());
			fos.write(1);
			fos.close();
			fos = null;
			
			this.fileHandlerSvc.updateFileTrackerListing(clientID, 
															tempDir.toFile().getAbsolutePath(),
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1");
			
			trackerList = this.fileDataSvc.getAllBackupTrackersForClient(clientID);

			assertEquals(3, trackerList.size());
			
			for(BackupFileTracker bft : trackerList) {
				if(bft.getFileName().startsWith("trackerListingTest3")) {
					assertTrue(bft.isFileChanged() && bft.isFileNew());
				} else if(bft.getFileName().startsWith("trackerListingTest2")) {
				   assertTrue(bft.isFileChanged());
				} else if(bft.getFileName().startsWith("trackerListingTest1")) {
				   assertTrue(bft.isFileDeleted());
				}
			}
				
		} catch (Exception e) {
			logger.error("Error: ", e);
			e.printStackTrace();		
		} finally {
			if( null != fos ) {
				fos.close();
			}
		}		
	}
	
	@Test
	public void sendBatchToProcessingQueue() {
		
		UUID clientID = UUID.randomUUID();
		
		try {
			
			BackupFileDataBatch dataBatch1 = new BackupFileDataBatch(clientID);
			BackupFileDataBatch dataBatch2 = new BackupFileDataBatch(clientID);
			
			dataBatch1 = this.fileDataSvc.addBackupFileDataBatch(dataBatch1);
			dataBatch2 = this.fileDataSvc.addBackupFileDataBatch(dataBatch2);
						
			BackupFileDataPacket dataPacket1 = new BackupFileDataPacket(dataBatch1.getFileBatchID(), 1L, 10, 1, 10, "/foo/bar", "file1.txt", FileAction.Create);
			BackupFileDataPacket dataPacket2 = new BackupFileDataPacket(dataBatch1.getFileBatchID(), 1L, 10, 1, 10, "/foo/bar", "file1.txt", FileAction.Create);
			BackupFileDataPacket dataPacket3 = new BackupFileDataPacket(dataBatch2.getFileBatchID(), 2L, 10, 1, 10, "/foo/bar", "file3.txt", FileAction.Update);
						
			dataPacket1 = fileDataSvc.addBackupFileDataPacket(dataPacket1);
			dataPacket2 = fileDataSvc.addBackupFileDataPacket(dataPacket2);
			dataPacket3 = fileDataSvc.addBackupFileDataPacket(dataPacket3);
		
			fileHandlerSvc.sendBatchToProcessingQueue(dataBatch1);
			fileHandlerSvc.sendBatchToProcessingQueue(dataBatch2);
			
			dataBatch1 = fileDataSvc.getDataBatchByBatchID(dataBatch1.getFileBatchID());
			dataBatch2 = fileDataSvc.getDataBatchByBatchID(dataBatch2.getFileBatchID());
						
			assertNotNull(dataBatch1.getDateTimeSent());
			assertNotNull(dataBatch2.getDateTimeSent());
			
			assertNull(dataBatch1.getDateTimeConfirmed());
			assertNull(dataBatch2.getDateTimeConfirmed());
			
			List<BackupFileDataPacket> packetList = ((PacketHandlerQueueTest)this.packetHandlerQueueTest).getPacketQueue();
			
			assertEquals(3, packetList.size());
			
			int b1Count = 0;
			int b2Count = 0;
			
			for(BackupFileDataPacket packet : packetList) {
				if( packet.getFileBatchID() == dataBatch1.getFileBatchID() ) {
					b1Count += 1;
				} else if( packet.getFileBatchID().longValue() == dataBatch2.getFileBatchID().longValue() ) {
					b2Count += 1;
				}
			}
			
			assertEquals(2, b1Count);
			assertEquals(1, b2Count);
			
			this.fileHandlerSvc.removeBatchFromProcessingQueue(dataBatch1.getFileBatchID());
						
			packetList = ((PacketHandlerQueueTest)this.packetHandlerQueueTest).getPacketQueue();
			
			assertEquals(1, packetList.size());
			
			assertEquals(dataBatch2.getFileBatchID(), packetList.get(0).getFileBatchID());
			
			dataBatch1 = this.fileDataSvc.addBackupFileDataBatch(dataBatch1);
			
			assertNull(dataBatch1.getDateTimeSent());
			
		} catch (Exception e) {
			logger.error("Error: ", e);
			e.printStackTrace();
		}	
		
	}

	@Test
	public void readFromProcessingQueue() {
		
		UUID clientID = UUID.randomUUID();
		
		try {
			
			BackupFileDataBatch dataBatch1 = new BackupFileDataBatch(clientID);
			BackupFileDataBatch dataBatch2 = new BackupFileDataBatch(clientID);
			
			dataBatch1 = this.fileDataSvc.addBackupFileDataBatch(dataBatch1);
			dataBatch2 = this.fileDataSvc.addBackupFileDataBatch(dataBatch2);
						
			BackupFileDataPacket dataPacket1 = new BackupFileDataPacket(dataBatch1.getFileBatchID(), 1L, 10, 1, 10, "/foo/bar", "file1.txt", FileAction.Create);
			BackupFileDataPacket dataPacket2 = new BackupFileDataPacket(dataBatch1.getFileBatchID(), 1L, 10, 1, 10, "/foo/bar", "file1.txt", FileAction.Create);
			BackupFileDataPacket dataPacket3 = new BackupFileDataPacket(dataBatch2.getFileBatchID(), 2L, 10, 1, 10, "/foo/bar", "file3.txt", FileAction.Update);
						
			dataPacket1 = fileDataSvc.addBackupFileDataPacket(dataPacket1);
			dataPacket2 = fileDataSvc.addBackupFileDataPacket(dataPacket2);
			dataPacket3 = fileDataSvc.addBackupFileDataPacket(dataPacket3);
		
			fileHandlerSvc.sendBatchToProcessingQueue(dataBatch1);
			fileHandlerSvc.sendBatchToProcessingQueue(dataBatch2);
			
			List<BackupFileDataPacket> packetList = this.fileHandlerSvc.retrieveBatchFromProcessingQueue(dataBatch1.getFileBatchID());
			
			assertNotNull(packetList);
			assertEquals(2, packetList.size());
			assertEquals(FileAction.Create, packetList.get(0).getFileAction());
			assertEquals(FileAction.Create, packetList.get(1).getFileAction());
			
			assertNotNull(dataBatch1.getDateTimeCaptured());
			assertNotNull(dataBatch1.getDateTimeSent());
			assertNotNull(dataBatch1.getDateTimeConfirmed());
			
		} catch (Exception e) {
			logger.error("Error: ", e);
			e.printStackTrace();
		}
	}
}
