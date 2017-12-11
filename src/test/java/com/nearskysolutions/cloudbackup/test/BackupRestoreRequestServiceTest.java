package com.nearskysolutions.cloudbackup.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.FilePacketHandlerQueue;
import com.nearskysolutions.cloudbackup.common.RestoreRequestHandlerQueue;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.NotifyType;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.BackupRestoreRequestService;
import com.nearskysolutions.cloudbackup.test.beans.PacketHandlerQueueTest;

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

	@Autowired
	@Qualifier("RestoreRequestHandlerQueue")
	private RestoreRequestHandlerQueue restoreRequestHandlerTest;
	
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

		this.clientFileTracker1.setTrackerStatus(BackupFileTrackerStatus.Stored);
		this.clientFileTracker2.setTrackerStatus(BackupFileTrackerStatus.Stored);
		
		this.backupDataSvc.updateBackupFileTracker(this.clientFileTracker1);
		this.backupDataSvc.updateBackupFileTracker(this.clientFileTracker2);
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
	public void addRestoreRequest() {
		
		try {
									
			String submitterId = "test user";
			String notifyTarget = "test target";
			String notifyParameter = "test parameter";
			boolean bIncludeSubdirectories = true;
			
			BackupRestoreRequest origRestoreRequest = createBackupRestoreRequest();
			origRestoreRequest.setSubmitterId(submitterId);
			origRestoreRequest.setNotifyTarget(notifyTarget);
			origRestoreRequest.setNotifyParameter(notifyParameter);
			origRestoreRequest.setIncludeSubdirectories(bIncludeSubdirectories);
			
			BackupRestoreRequest newRestoreRequest =  restoreSvc.addRestoreRequest(origRestoreRequest);
												
			compareRestoreRequests(newRestoreRequest, origRestoreRequest, true);			
			
			
			assertEquals(RestoreStatus.Pending, origRestoreRequest.getCurrentStatus());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}
		
	}
	
	@Test
	public void queryRestoreRequestByRequestID() {
		try {									
			BackupRestoreRequest origRestoreRequest = createBackupRestoreRequest();
			
			origRestoreRequest =  restoreSvc.addRestoreRequest(origRestoreRequest);
												
			BackupRestoreRequest newRestoreRequest = restoreSvc.getRestoreRequestByRequestID(origRestoreRequest.getRequestID());
			
			compareRestoreRequests(newRestoreRequest, origRestoreRequest, false);
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
																		String.format("test parameter %s", this.backupClient.getClientID().toString()),
																		false);
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
	public void updateRestoreRequest() {
								
		try {			
			BackupRestoreRequest origRestoreRequest = createBackupRestoreRequest();			
									
			origRestoreRequest = restoreSvc.addRestoreRequest(origRestoreRequest);			
			
			origRestoreRequest.setSubmitterId("new submitter");
			//TODO Add when more than one type is supported
			//origRestoreRequest.setNotifyType(NotifyType.Email);
			origRestoreRequest.setNotifyTarget("new notify target");
			origRestoreRequest.setNotifyParameter("new notify parameter");
			origRestoreRequest.setIncludeSubdirectories(!origRestoreRequest.isIncludeSubdirectories());
			origRestoreRequest.getRequestedFileTrackerIDs().clear();;
			origRestoreRequest.getRequestedFileTrackerIDs().add(1001L);
			origRestoreRequest.getRequestedFileTrackerIDs().add(2001L);
			origRestoreRequest.getRequestedFileTrackerIDs().add(3001L);
			
			Calendar cal = Calendar.getInstance();
			
			origRestoreRequest.setSubmittedDateTime(cal.getTime());
			
			cal.add(Calendar.SECOND, 100);
			
			origRestoreRequest.setProcessingStartDateTime(cal.getTime());
			
			cal.add(Calendar.SECOND, 200);
			
			origRestoreRequest.setCompletedDateTime(cal.getTime());
			
			origRestoreRequest.setErrorMessage("new error");
			
			origRestoreRequest.setRestoreResultsURLs(new ArrayList<String>());
			origRestoreRequest.getRestoreResultsURLs().add("New URL 1");
			origRestoreRequest.getRestoreResultsURLs().add("New URL 2");
			
			origRestoreRequest.setCurrentStatus(RestoreStatus.Success);	
			
			BackupRestoreRequest newRestoreRequest = restoreSvc.updateRestoreRequest(origRestoreRequest);
			
			compareRestoreRequests(newRestoreRequest, origRestoreRequest, false);			
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}		
	}
		
	@Test
	public void generateValidationExceptions() {
						
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
			
			restoreRequest.setSubmitterId("test submitter");
						
			this.clientFileTracker1.setTrackerStatus(BackupFileTrackerStatus.Pending);
			
			this.backupDataSvc.updateBackupFileTracker(this.clientFileTracker1);
			
			try {
				restoreSvc.addRestoreRequest(restoreRequest);
		        fail("Expected a validation exception to be thrown");
		    } catch (Exception ex) {
		        assertThat(ex.getMessage(), containsString("Invalid status for tracker ID"));
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


	@Test
	public void sendRequestToProcessingQueue() {
						
		try {
			
			BackupRestoreRequest restoreRequest1 = createBackupRestoreRequest();
			BackupRestoreRequest restoreRequest2 = createBackupRestoreRequest();
			
			restoreRequest1 = this.restoreSvc.addRestoreRequest(restoreRequest1);
			
			assertTrue(this.restoreRequestHandlerTest.queueHasRequests());
			
			restoreRequest2 = this.restoreSvc.addRestoreRequest(restoreRequest2);

			BackupRestoreRequest queuedRequest1 = this.restoreRequestHandlerTest.retreiveNextRestoreRequest();
			BackupRestoreRequest queuedRequest2 = this.restoreRequestHandlerTest.retreiveNextRestoreRequest();
			
			assertFalse(this.restoreRequestHandlerTest.queueHasRequests());
			
			assertEquals(queuedRequest1.getRequestID(), restoreRequest1.getRequestID());
			assertEquals(queuedRequest2.getRequestID(), restoreRequest2.getRequestID());
			
			assertNotNull(queuedRequest1.getSubmittedDateTime());
			assertNotNull(queuedRequest2.getSubmittedDateTime());		
					
		} catch (Exception e) {
			logger.error("Error: ", e);
			e.printStackTrace();
			
			fail(String.format("Error: %s", e.getMessage()));
		}			
	}
	

	private void compareRestoreRequests(BackupRestoreRequest newRestoreRequest, BackupRestoreRequest origRestoreRequest, boolean isNewRequest) throws Exception {
		
		assertNotNull(newRestoreRequest);
		assertEquals(newRestoreRequest.getClientID(), origRestoreRequest.getClientID());		
		assertEquals(newRestoreRequest.getSubmitterId(), origRestoreRequest.getSubmitterId());
		assertEquals(newRestoreRequest.getNotifyType(), origRestoreRequest.getNotifyType());
		assertEquals(newRestoreRequest.getNotifyTarget(), origRestoreRequest.getNotifyTarget());
		assertEquals(newRestoreRequest.getNotifyParameter(), origRestoreRequest.getNotifyParameter());
		assertEquals(newRestoreRequest.isIncludeSubdirectories(), origRestoreRequest.isIncludeSubdirectories());
		assertNotNull(newRestoreRequest.getRequestedFileTrackerIDs());
		assertEquals(newRestoreRequest.getRequestedFileTrackerIDs().size(), origRestoreRequest.getRequestedFileTrackerIDs().size());
		
		for(int i = 0; i < newRestoreRequest.getRequestedFileTrackerIDs().size(); i++) {				
			assertEquals(newRestoreRequest.getRequestedFileTrackerIDs().get(i), origRestoreRequest.getRequestedFileTrackerIDs().get(i));
		}			
		
		if( isNewRequest ) {
			assertNotNull(newRestoreRequest.getSubmittedDateTime());
			assertNull(newRestoreRequest.getProcessingStartDateTime());
			assertNull(newRestoreRequest.getCompletedDateTime());
			assertNull(newRestoreRequest.getErrorMessage());
			assertNull(newRestoreRequest.getRestoreResultsURLs());
		} else {
			assertEquals(newRestoreRequest.getRequestID(), origRestoreRequest.getRequestID());
			assertEquals(newRestoreRequest.getSubmittedDateTime(), origRestoreRequest.getSubmittedDateTime());
			assertEquals(newRestoreRequest.getCompletedDateTime(), origRestoreRequest.getCompletedDateTime());
			assertEquals(newRestoreRequest.getProcessingStartDateTime(), origRestoreRequest.getProcessingStartDateTime());
			assertEquals(newRestoreRequest.getCurrentStatus(), origRestoreRequest.getCurrentStatus());
			
			if( null == origRestoreRequest.getRestoreResultsURLs() ) {
				assertNull(newRestoreRequest.getRestoreResultsURLs());
			} else {
				assertNotNull(newRestoreRequest.getRestoreResultsURLs());
				
				assertEquals(newRestoreRequest.getRestoreResultsURLs().size(), origRestoreRequest.getRestoreResultsURLs().size());
				
				for(int i = 0; i < newRestoreRequest.getRestoreResultsURLs().size(); i++) {				
					assertEquals(newRestoreRequest.getRestoreResultsURLs().get(i), origRestoreRequest.getRestoreResultsURLs().get(i));
				}				
			}
		}		
	}

}
