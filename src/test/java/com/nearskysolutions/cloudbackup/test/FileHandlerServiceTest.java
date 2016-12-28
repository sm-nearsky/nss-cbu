package com.nearskysolutions.cloudbackup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.data.BackupFileTrackerRepository;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.FileHandlerService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class FileHandlerServiceTest {

Logger logger = LoggerFactory.getLogger(FileHandlerServiceTest.class);
	
	@Autowired	
	private FileHandlerService fileHandlerSvc;
				
	@Autowired
	private BackupFileDataService fileDataSvc;
	
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
			
			this.fileHandlerSvc.updateFileTrackerListing(clientID, tempDir.toFile().getAbsolutePath());
			
			List<BackupFileTracker> trackerList = this.fileDataSvc.getAllBackupTrackersForClient(clientID);
			
			assertEquals(2, trackerList.size());
			
			testFile3 = Files.createTempFile(tempDir, "trackerListingTest3", null, new FileAttribute<?>[0]);
			testFile3.toFile().deleteOnExit();
			
			BackupFileTracker t3 = new BackupFileTracker(clientID, 
															"Repository Type 1", 
															"Repository Location 1", 
															"Repository Key 1", 
															testFile3.toFile().getAbsolutePath());
			
			fileDataSvc.addBackupFileTracker(t3);
			
			this.fileHandlerSvc.updateFileTrackerListing(clientID, tempDir.toFile().getAbsolutePath());
			
			trackerList = this.fileDataSvc.getAllBackupTrackersForClient(clientID);

			assertEquals(3, trackerList.size());
			
			testFile1.toFile().delete();
			testFile1 = null;
			
			fos = new FileOutputStream(testFile2.toFile());
			fos.write(1);
			fos.close();
			fos = null;
			
			this.fileHandlerSvc.updateFileTrackerListing(clientID, tempDir.toFile().getAbsolutePath());
			
			trackerList = this.fileDataSvc.getAllBackupTrackersForClient(clientID);

			assertEquals(3, trackerList.size());
			
			for(BackupFileTracker bft : trackerList) {
				if(bft.getFileName().startsWith("trackerListingTest3")) {
					assertTrue(!bft.isFileChanged() && !bft.isFileChanged());
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
		
}
