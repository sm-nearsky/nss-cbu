package com.nearskysolutions.cloudbackup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.NotifyType;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;
import com.nearskysolutions.cloudbackup.data.BackupFileClientRepository;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
public class BackupAdminServiceTest {

	private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));	
	
	private Gson gson;
	
	@Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    
    @Autowired
	private BackupFileDataService backupDataSvc;
    
    @Autowired
	private BackupFileClientService backupClientSvc;
        
    private BackupFileClient backupClient;
	private BackupFileTracker clientFileTracker1 = null;
	private BackupFileTracker clientFileTracker2 = null;
	
    Logger logger = LoggerFactory.getLogger(BackupAdminServiceTest.class);
    
    @Before
    public void initTests() {
    	this.gson = createGsonParser();
    	
        DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
        
    }
    
    @Test
    public void testClientAdmin() throws Exception {

    	try {
    	
	    	MvcResult results;
	    	BackupFileClient[] clientArr;
	    	BackupFileClient testClient1;
	    	BackupFileClient testClient2;    	
	    	String clientID;
	    	    	    	
	    	logger.info("Getting list of backup clients");
	    	
	    	results = mockMvc.perform(get("/clients"))
								        .andExpect(status().isOk())
								        .andReturn();
	
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	clientArr = this.gson.fromJson(results.getResponse().getContentAsString(), BackupFileClient[].class);

	    	assertEquals(0, clientArr.length);
	    	
	    	testClient1 = new BackupFileClient("test name", 
											   "test desc", 
											   "test rep", 
											   "test loc", 
											   "test rep", 
											   new ArrayList<String>());
	    	
	    	testClient1.getDirectoryIncludes().add("backupdir1");
	    	testClient1.getDirectoryIncludes().add("backupdir2");
	    	
	    	logger.info(String.format("Created test client 1: %s", testClient1.toString()));
	    	
	    	logger.info("Calling create service for test client 1");
	    	
	    	results = mockMvc.perform(put("/clients")
						                .contentType(contentType)
						                .content(this.gson.toJson(testClient1)))
						                .andExpect(status().isCreated())
						                .andReturn();
	    	            
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	logger.info("Getting list of clients again");
	    	
	    	results = mockMvc.perform(get("/clients")
							                .accept(MediaType.APPLICATION_JSON))
							                .andExpect(status().isOk())                
							                .andReturn();                
			
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    			
	    	clientArr = this.gson.fromJson(results.getResponse().getContentAsString(), BackupFileClient[].class);
	    	    	
	    	assertEquals(1, clientArr.length);
	    	
	    	compareBackupClient(clientArr[0], testClient1, true);
	    	
	    	testClient1 = clientArr[0];
	    	
	    	clientID = testClient1.getClientID().toString();
	    	
	    	logger.info(String.format("Checking for direct query of clientID: %s", clientID));
	    	
	    	results = mockMvc.perform(get(String.format("/clients/%s",clientID))
	                .accept(MediaType.APPLICATION_JSON))
	                .andExpect(status().isOk())
	                .andReturn();   
	    	
	    	testClient2 = this.gson.fromJson(results.getResponse().getContentAsString(), BackupFileClient.class);
	    	    	
	    	compareBackupClient(testClient2, testClient1, true);
	    	    	    	
	    	testClient2.setClientName("new name");
	    	testClient2.setClientDescription("new desc");
	    	testClient2.setCurrentRepositoryKey("new key");
	    	testClient2.setCurrentRepositoryLocation("new loc");
	    	testClient2.setCurrentRepositoryType("new type");
	    	testClient2.setDirectoryIncludes(new ArrayList<String>());
	    	testClient2.getDirectoryIncludes().add("new dir");
	    	    	
	    	logger.info(String.format("Created test client 2 (update of test client 1): %s", testClient2.toString()));
	    	
	    	logger.info(String.format("Calling put to update client with ID: %s", testClient2.getClientID().toString()));
	    	
	    	results = mockMvc.perform(put("/clients")
						                .contentType(contentType)
						                .content(this.gson.toJson(testClient2)))
						                .andExpect(status().isOk())
						                .andReturn();
	    	
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	logger.info("Getting list of clients after update");
	    	
	    	results = mockMvc.perform(get("/clients")
	                .accept(MediaType.APPLICATION_JSON))
	                .andExpect(status().isOk())                
	                .andReturn();
	    	    	
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	clientArr = this.gson.fromJson(results.getResponse().getContentAsString(), BackupFileClient[].class);
	    		    	
	    	assertEquals(1, clientArr.length);
	    	
	    	compareBackupClient(clientArr[0], testClient2, false);
	    	
	    	logger.info(String.format("Calling delete for client with ID: %s", testClient2.getClientID().toString()));
	    	
	    	results = mockMvc.perform(delete(String.format("/clients/%s",testClient2.getClientID()))
						                .accept(MediaType.APPLICATION_JSON))
						                .andExpect(status().isOk())
						                .andReturn(); 
	    	
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	logger.info("Getting list of clients after update");
	    	
	    	results = mockMvc.perform(get("/clients")
						                .accept(MediaType.APPLICATION_JSON))
						                .andExpect(status().isOk())                
						                .andReturn();
	    	    	
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	clientArr = this.gson.fromJson(results.getResponse().getContentAsString(), BackupFileClient[].class);
	    	
	    	assertEquals(0, clientArr.length);
	    	
	    	logger.info("Correct result of no clients found after delete");
	    	
	//    	results = mockMvc.perform(get(String.format("/clients/%s",clientID))
	//                .accept(MediaType.APPLICATION_JSON))
	//                .andExpect(status().isOk())                
	//                .andExpect(jsonPath("$.clientName", is(testClient.getClientName())))
	//                .andExpect(jsonPath("$.clientDescription", is(testClient.getClientDescription())))
	//                .andExpect(jsonPath("$.currentRepositoryType", is(testClient.getCurrentRepositoryType())))
	//                .andExpect(jsonPath("$.currentRepositoryLocation", is(testClient.getCurrentRepositoryLocation())))
	//                .andExpect(jsonPath("$.currentRepositoryKey", is(testClient.getCurrentRepositoryKey())))
	//                .andExpect(jsonPath("$.directoryIncludes[0]", is(testClient.getDirectoryIncludes().get(0))))
	//                .andExpect(jsonPath("$.directoryIncludes[1]", is(testClient.getDirectoryIncludes().get(1))))
	//                .andReturn(); 

    	} catch(Exception ex) {
    		logger.error("Error: ", ex);
    		
    		fail(ex.getMessage());    		
    	}
    }
    
    @Test
    public void testRestoreAdmin() throws Exception {

    	try
    	{
    		initRestoreTests();
    	
	    	MvcResult results;
	    	BackupRestoreRequest[] requestArr;
	    	BackupRestoreRequest restoreRequest1;
	    	BackupRestoreRequest restoreRequest2;
	    	String requestID;
	    	    	    	
	    	logger.info("Getting list of restoreRequests");
	    	
	    	results = mockMvc.perform(get("/restoreRequests"))
								        .andExpect(status().isOk())
								        .andReturn();
	
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	requestArr = this.gson.fromJson(results.getResponse().getContentAsString(), BackupRestoreRequest[].class);
	    	
	    	assertEquals(0, requestArr.length);
	    	    	
	    	ArrayList<Long> trackerIdList = new ArrayList<Long>();
	    	trackerIdList.add(this.clientFileTracker1.getBackupFileTrackerID());
	    	trackerIdList.add(this.clientFileTracker2.getBackupFileTrackerID());
	    	
	    	restoreRequest1 = new BackupRestoreRequest( this.backupClient.getClientID(),
	    												"test submitter1", 
	    												trackerIdList, 
														NotifyType.None, 
														"test target 1", 
														"test parameter 1");
	    	    	
	    	logger.info(String.format("Created restore request 1: %s", restoreRequest1.toString()));
	    	
	    	logger.info("Calling create service for restore request 1");
	    	    	    	
	    	results = mockMvc.perform(put("/restoreRequests")
						                .contentType(contentType)
						                .content(this.gson.toJson(restoreRequest1)))
						                .andExpect(status().isCreated())
						                .andReturn();
	    	            
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	logger.info("Getting list of requests again");
	    	
	    	results = mockMvc.perform(get("/restoreRequests")
							                .accept(MediaType.APPLICATION_JSON))
							                .andExpect(status().isOk())                
							                .andReturn();                
			
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    			
	    	requestArr = this.gson.fromJson(results.getResponse().getContentAsString(), BackupRestoreRequest[].class);
	    	    	
	    	assertEquals(1, requestArr.length);
	    	
	    	compareRestoreRequest(requestArr[0], restoreRequest1, true);
	    	
	    	restoreRequest1 = requestArr[0];
	    	
	    	requestID = restoreRequest1.getRequestID().toString();
	    	
	    	logger.info(String.format("Checking for direct query of requestID: %s", requestID));
	    	
	    	results = mockMvc.perform(get(String.format("/restoreRequests/%s",requestID))
						                .accept(MediaType.APPLICATION_JSON))
						                .andExpect(status().isOk())
						                .andReturn();   
	    	
	    	restoreRequest2 = this.gson.fromJson(results.getResponse().getContentAsString(), BackupRestoreRequest.class);
	    	    	
	    	compareRestoreRequest(restoreRequest2, restoreRequest1, false);
	    	
	    	
	    	logger.info(String.format("Calling cancel for restore request with ID: %s", requestID.toString()));
	    	
	    	results = mockMvc.perform(delete(String.format("/restoreRequests/%s", requestID))
						                .accept(MediaType.APPLICATION_JSON))
						                .andExpect(status().isOk())
						                .andReturn(); 
	    	
	    	logger.info(String.format("Received result: %d", results.getResponse().getStatus()));
	    	
	    	logger.info("Getting restore request after cancel");
	    	
	    	results = mockMvc.perform(get(String.format("/restoreRequests/%s",requestID))
	                .accept(MediaType.APPLICATION_JSON))
	                .andExpect(status().isOk())
	                .andReturn();   
	
	    	restoreRequest1 = this.gson.fromJson(results.getResponse().getContentAsString(), BackupRestoreRequest.class);
	
	    	assertEquals(RestoreStatus.Cancelled, restoreRequest1.getCurrentStatus());
	    	
	    	logger.info("Correct result of no active requests found after delete");
	    	
    	} catch(Exception ex) {
    		logger.error("Error: ", ex);
    		
    		fail(ex.getMessage());    		
    	} finally {
    		try {
    			cleanUpAfterRestoreTests();
    		} catch (Exception ex){
    			logger.error("Error: ", ex);
    			
    			fail(ex.getMessage());	
    		}
    	}
    }
    
	private void compareBackupClient(BackupFileClient compareClient, BackupFileClient testClient, boolean newClient) {    	
    	
    	if(false == newClient) {    		
    		assertEquals(testClient.getClientID(), compareClient.getClientID());
    		assertEquals(testClient.getCreatedDateTime(), compareClient.getCreatedDateTime());
    	}
    	
    	assertEquals(testClient.getClientName(), compareClient.getClientName());
		assertEquals(testClient.getClientDescription(), compareClient.getClientDescription());
		assertEquals(testClient.getCurrentRepositoryType(), compareClient.getCurrentRepositoryType());
		assertEquals(testClient.getCurrentRepositoryLocation(), compareClient.getCurrentRepositoryLocation());
		assertEquals(testClient.getCurrentRepositoryKey(), compareClient.getCurrentRepositoryKey());
				
		if( null == testClient.getDirectoryIncludes() ) {			
			assertNull(compareClient.getDirectoryIncludes());			
		} else {
			assertNotNull(compareClient.getDirectoryIncludes());
			assertEquals(testClient.getDirectoryIncludes().size(), compareClient.getDirectoryIncludes().size());
			
			for(int i = 0; i < testClient.getDirectoryIncludes().size(); i++) {
				assertEquals(testClient.getDirectoryIncludes().get(i), compareClient.getDirectoryIncludes().get(i));
			}	
		}
    }
	
	private void compareRestoreRequest(BackupRestoreRequest compareRequest, BackupRestoreRequest testRequest, boolean newRequest) {
		
		if( false == newRequest ) {
			assertEquals(compareRequest.getRequestID(), testRequest.getRequestID());
			assertEquals(compareRequest.getSubmittedDateTime(), testRequest.getSubmittedDateTime());
			assertEquals(compareRequest.getCurrentStatus(), testRequest.getCurrentStatus());			
			assertEquals(compareRequest.getProcessingStartDateTime(), testRequest.getProcessingStartDateTime());
			assertEquals(compareRequest.getCompletedDateTime(), testRequest.getCompletedDateTime());
			assertEquals(compareRequest.getErrorMessage(), testRequest.getErrorMessage());	
			
			if( null == testRequest.getRestoreResultsURLs() ) {			
				assertNull(compareRequest.getRestoreResultsURLs());			
			} else {
				assertNotNull(compareRequest.getRestoreResultsURLs());
				assertEquals(testRequest.getRestoreResultsURLs().size(), compareRequest.getRestoreResultsURLs().size());
				
				for(int i = 0; i < testRequest.getRestoreResultsURLs().size(); i++) {
					assertEquals(testRequest.getRestoreResultsURLs().get(i), compareRequest.getRestoreResultsURLs().get(i));
				}	
			}
		}
		
		assertEquals(compareRequest.getSubmitterId(), testRequest.getSubmitterId());
		assertEquals(compareRequest.getNotifyType(), testRequest.getNotifyType());
		assertEquals(compareRequest.getNotifyTarget(), testRequest.getNotifyTarget());
		assertEquals(compareRequest.getNotifyParameter(), testRequest.getNotifyParameter());		
		
		if( null == testRequest.getRequestedFileTrackerIDs() ) {			
			assertNull(compareRequest.getRequestedFileTrackerIDs());			
		} else {
			assertNotNull(compareRequest.getRequestedFileTrackerIDs());
			assertEquals(testRequest.getRequestedFileTrackerIDs().size(), compareRequest.getRequestedFileTrackerIDs().size());
			
			for(int i = 0; i < testRequest.getRequestedFileTrackerIDs().size(); i++) {
				assertEquals(testRequest.getRequestedFileTrackerIDs().get(i), compareRequest.getRequestedFileTrackerIDs().get(i));
			}	
		}
		
		
		
	}

	private void initRestoreTests() throws Exception {
    	
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

	public void cleanUpAfterRestoreTests() throws Exception {
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
		
		try
		{
			if( null != this.backupClient ) {
				this.backupClientSvc.deleteBackupClient(this.backupClient.getClientID());
			}
		} catch(Exception ex) {
			logger.error("Could not delete backup client", ex);
			
			throw ex;
		}
	}
    private Gson createGsonParser() {
    	// Creates the json object which will manage the information received 
    	GsonBuilder builder = new GsonBuilder(); 

    	// Register an adapter to manage the date types as long values 
    	builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {

			@Override
			public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				return new Date(json.getAsJsonPrimitive().getAsLong()); 
			}	     	    
    	});
    	
    	// Register an adapter to manage the date types as long values 
    	builder.registerTypeAdapter(Date.class, new com.google.gson.JsonSerializer<Date>() {

			@Override
			public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {				
				return new JsonPrimitive(src.getTime());
			}
				     	    
    	});

    	return builder.create();
    }
}
