package com.nearskysolutions.cloudbackup.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.NotifyType;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.BackupRestoreRequestService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class BackupRestoreRequestServiceTest {

Logger logger = LoggerFactory.getLogger(BackupRestoreRequestServiceTest.class);
	
	@Autowired	
	private BackupRestoreRequestService restoreSvc;
	
	@Autowired
	private BackupFileClientService backupClientSvc;
	
	@Autowired
	private BackupFileDataService backupDataSvc;

	private BackupFileClient backupClient;
	private BackupFileTracker clientFileTracker1 = null;
	private BackupFileTracker clientFileTracker2 = null;
	
	@Before
	public void initTests() throws Exception {
		UUID randomID = UUID.randomUUID();
		ArrayList<String> dirList = new ArrayList<String>();
		
		File tmpFile1 = File.createTempFile(String.format("file1_%s", randomID.toString()), "tmp");
		File tmpFile2 = File.createTempFile(String.format("file2_%s", randomID.toString()), "tmp");
		
		dirList.add(tmpFile1.getAbsolutePath());
		dirList.add(tmpFile1.getAbsolutePath());
		
		this.backupClient = this.backupClientSvc.addBackupClient(new BackupFileClient("Client " + randomID.toString(), 
												 										 "desc " + randomID.toString(), 
												 										 "rep "+ randomID.toString(), 
												 										 "loc "+	randomID.toString(), 
																						 "key " + randomID.toString(),
																						 dirList));		
		
		this.clientFileTracker1 = this.backupDataSvc.addBackupFileTracker(new BackupFileTracker(this.backupClient.getClientID(), 
																		   this.backupClient.getCurrentRepositoryType(),
																		   this.backupClient.getCurrentRepositoryLocation(),
																		   this.backupClient.getCurrentRepositoryKey(),
																		   tmpFile1.getAbsolutePath()));

		this.clientFileTracker2 = this.backupDataSvc.addBackupFileTracker(new BackupFileTracker(this.backupClient.getClientID(), 
																		   this.backupClient.getCurrentRepositoryType(),
																		   this.backupClient.getCurrentRepositoryLocation(),
																		   this.backupClient.getCurrentRepositoryKey(),
																		   tmpFile2.getAbsolutePath()));
	}
	
	@After
	public void completeTests() throws Exception{
		try
		{
			if( null != clientFileTracker1 && clientFileTracker1.getFileReference().exists() ) {
				clientFileTracker1.getFileReference().delete();
			}			
		} catch(Exception ex) {
			logger.error("Could not delete client temp file 1", ex);
			
			throw ex;
		}
		
		try
		{
			if( null != clientFileTracker2 && clientFileTracker2.getFileReference().exists() ) {
				clientFileTracker2.getFileReference().delete();
			}			
		} catch(Exception ex) {
			logger.error("Could not delete client temp file 2", ex);
			
			throw ex;
		}
	}
	
	@Test
	public void queryRestoreRequestByRequestID() {
		
		try {
									
			String submitterId = "test user";
			String notifyTarget = "test target";
			String notifyParameter = "test parameter";
						
			BackupRestoreRequest restoreRequest = createBackupRestoreRequest();
			restoreRequest.setSubmitterId(submitterId);
			restoreRequest.setNotifyTarget(notifyTarget);
			restoreRequest.setNotifyParameter(notifyParameter);
			
			restoreRequest = restoreSvc.addRestoreRequest(restoreRequest);
							
			UUID requestID = restoreRequest.getRequestID();
			
			restoreRequest = restoreSvc.getRestoreRequestByRequestID(requestID);
			
			assertNotNull(restoreRequest);
			assertEquals(this.backupClient.getClientID(), restoreRequest.getClientID());
			assertEquals(requestID, restoreRequest.getRequestID());
			assertEquals(submitterId, restoreRequest.getSubmitterId());
			assertEquals(NotifyType.None, restoreRequest.getNotifyType());
			assertEquals(notifyTarget, restoreRequest.getNotifyTarget());
			assertEquals(notifyParameter, restoreRequest.getNotifyParameter());
			assertNotNull(restoreRequest.getRequestedFileTrackerIDs());
			assertEquals(restoreRequest.getRequestedFileTrackerIDs().size(), restoreRequest.getRequestedFileTrackerIDs().size());
			
			for(int i = 0; i < restoreRequest.getRequestedFileTrackerIDs().size(); i++) {				
				assertEquals(restoreRequest.getRequestedFileTrackerIDs().get(i), restoreRequest.getRequestedFileTrackerIDs().get(i));
			}			
			
			assertNotNull(restoreRequest.getSubmittedDateTime());
			
			assertEquals(RestoreStatus.Pending, restoreRequest.getCurrentStatus());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}
		
	}
	
	@Test
	public void queryAllRestoreRequests() {
								
		try {		
			
			
			BackupRestoreRequest restoreRequest1 = createBackupRestoreRequest();
			restoreRequest1.setSubmitterId("submitter 1");
			
			BackupRestoreRequest restoreRequest2 = createBackupRestoreRequest();
			restoreRequest2.setSubmitterId("submitter 2");
			
			BackupRestoreRequest restoreRequest3 = createBackupRestoreRequest();
			restoreRequest2.setSubmitterId("submitter 3");

			
			restoreSvc.addRestoreRequest(restoreRequest1);
			Thread.sleep(1000); //Wait to test sort
			
			restoreSvc.addRestoreRequest(restoreRequest2);
			Thread.sleep(1000); //Wait to test sort
			
			restoreSvc.addRestoreRequest(restoreRequest3);
	
			List<BackupRestoreRequest> requestList = restoreSvc.getAllRestoreRequests();
			
			assertNotNull(requestList);
			assertEquals(3, requestList.size());

			assertEquals(restoreRequest1.getSubmitterId(), requestList.get(0).getSubmitterId());
			assertEquals(restoreRequest2.getSubmitterId(), requestList.get(1).getSubmitterId());
			assertEquals(restoreRequest3.getSubmitterId(), requestList.get(2).getSubmitterId());
			
			assertTrue(requestList.get(0).getSubmittedDateTime().getTime() < requestList.get(1).getSubmittedDateTime().getTime());
			assertTrue(requestList.get(1).getSubmittedDateTime().getTime() < requestList.get(2).getSubmittedDateTime().getTime());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}		
	}

	private BackupRestoreRequest createBackupRestoreRequest() throws Exception {
		
		ArrayList<Long> trackerList = new ArrayList<Long>();

		trackerList.add(this.clientFileTracker1.getBackupFileTrackerID());
		trackerList.add(this.clientFileTracker2.getBackupFileTrackerID());
		
		BackupRestoreRequest restoreRequest = new BackupRestoreRequest(this.backupClient.getClientID(),
																		this.backupClient.getClientName(), 
																		trackerList, 
																		NotifyType.None, 
																		String.format("test target %s", this.backupClient.getClientID().toString()), 
																		String.format("test parameter %s", this.backupClient.getClientID().toString()));
		return restoreRequest;
	}
	
	@Test
	public void queryRestoreRequestsBySubmitter() {
								
		try {			
			List<Long> trackerList = new ArrayList<Long>();
			trackerList.add(101L);
			trackerList.add(102L);
									
			String submitterId1 = "submitter 1";
			String submitterId2 = "submitter 2";
							
			BackupRestoreRequest restoreRequest1 = createBackupRestoreRequest();
			restoreRequest1.setSubmitterId(submitterId1);
			
			BackupRestoreRequest restoreRequest2 = createBackupRestoreRequest();
			restoreRequest2.setSubmitterId(submitterId2);
			
			BackupRestoreRequest restoreRequest3 = createBackupRestoreRequest();
			restoreRequest3.setSubmitterId(submitterId2);
						
			restoreSvc.addRestoreRequest(restoreRequest1);			
			restoreSvc.addRestoreRequest(restoreRequest2);
			restoreSvc.addRestoreRequest(restoreRequest3);
		
			List<BackupRestoreRequest> requestList = restoreSvc.getRestoreRequestsBySubmitter(submitterId2);
			
			assertNotNull(requestList);
			assertEquals(2, requestList.size());
			
			assertEquals(submitterId2, requestList.get(0).getSubmitterId());
			assertEquals(submitterId2, requestList.get(1).getSubmitterId());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}		
	}
	
	@Test
	public void cancelRestoreRequest() {
								
		try {											
			BackupRestoreRequest restoreRequest = createBackupRestoreRequest();			
									
			restoreRequest = restoreSvc.addRestoreRequest(restoreRequest);			
			
			restoreSvc.cancelRestoreRequestForID(restoreRequest.getRequestID());
			
			restoreRequest = restoreSvc.getRestoreRequestByRequestID(restoreRequest.getRequestID());
			
			assertEquals(RestoreStatus.Cancelled, restoreRequest.getCurrentStatus());			
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}		
	}
	
	@Test
	public void generateValidationExceptinos() {
						
		try {	
			
			List<Long> trackerList = new ArrayList<Long>();
			trackerList.add(101L);
			trackerList.add(102L);
							
			BackupRestoreRequest restoreRequest = createBackupRestoreRequest();	
			
			restoreRequest.setSubmitterId(null);
			
			try {
				restoreSvc.addRestoreRequest(restoreRequest);
		        fail("Expected a validation exception to be thrown");
		    } catch (Exception ex) {
		        assertThat(ex.getMessage(), containsString("BackupRestoreRequest submitter ID can't be null or empty"));
		    }
			
			restoreRequest.setSubmitterId(" ");
			
			try {
				restoreSvc.addRestoreRequest(restoreRequest);
		        fail("Expected a validation exception to be thrown");
		    } catch (Exception ex) {
		        assertThat(ex.getMessage(), containsString("BackupRestoreRequest submitter ID can't be null or empty"));
		    }		
			
			
			//TODO Add if there is a way to set the completed state
//			restoreRequest.setSubmitterId("test");
//			
//			restoreRequest.setCurrentStatus(RestoreStatus.Success);
//			
//			try {
//				restoreSvc.cancelRestoreRequestForID(restoreRequest.getRequestID());
//		        fail("Expected a validation exception to be thrown");
//		    } catch (Exception ex) {
//		        assertThat(ex.getMessage(), containsString("Can't cancel request in completed state"));
//		    }
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}
		
	}
	
}
