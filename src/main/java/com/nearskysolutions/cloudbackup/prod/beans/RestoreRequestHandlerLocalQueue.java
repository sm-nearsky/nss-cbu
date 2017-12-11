package com.nearskysolutions.cloudbackup.prod.beans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.RestoreRequestHandlerQueue;

@Component(value="LocalRestoreRequestHandler")
public class RestoreRequestHandlerLocalQueue implements RestoreRequestHandlerQueue {

	Logger logger = LoggerFactory.getLogger(RestoreRequestHandlerLocalQueue.class);
			
	@Value( "${com.nearskysolutions.cloudbackup.queue.localRestoreRequestQueueDir}" )
	private String localRestoreRequestQueueDir;
	
	public RestoreRequestHandlerLocalQueue() {
			
	}
		
	@Override
	public boolean queueHasRequests() {
		
		logger.trace("Called RestoreRequestHandlerLocalQueue.queueHasRequests");
		
		File queueDir = new File(this.localRestoreRequestQueueDir);
		boolean retVal = false;
		
		if(false == queueDir.exists()) {
			logger.info("Restore request queue directory: %s does not exist, returning false from RestoreRequestHandlerLocalQueue.queueHasRequests");
		} else {
			//Note: Not checking for correct format, only files exist
			retVal = (queueDir.listFiles().length > 0);
		}
		
		logger.trace("Returning %s from RestoreRequestHandlerLocalQueue.queueHasRequests", retVal);
		
		return retVal;
	}

	@Override
	public void queueRequest(BackupRestoreRequest restoreRequest) throws Exception {
		
		logger.trace(String.format("Called RestoreRequestHandlerLocalQueue.queueRequest passing request: %s", restoreRequest));
		
		if( null == restoreRequest ) {
			throw new NullPointerException("BackupRestoreRequest reference can't be null");
		}
		
		logger.info(String.format("Creating local file queue entry for restoreRequest ID: %s", restoreRequest.getRequestID()));
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		
		try {			
						
			File parentDir = new File(this.localRestoreRequestQueueDir);
			
			if( false == parentDir.exists() ) {
				parentDir.mkdir();
			}
			
			//Save packet as XML and output
			String outputFileName = String.format("%s%srestoreRequestd_%s.xml", this.localRestoreRequestQueueDir, 
																						  File.separator, 
																						  restoreRequest.getRequestID().toString());
			
			fos = new FileOutputStream(outputFileName);
			
			logger.info(String.format("Writing object data to queue for restore request ID: %s:", restoreRequest.getRequestID()));

			JAXBContext jc = JAXBContext.newInstance( BackupRestoreRequest.class );
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(restoreRequest, fos);
			
			fos.close();
			fos = null;
			
		} finally {
			if( null != fis ) {
				fis.close();
			}
			
			if( null != fos ) {
				fos.close();
			}
		}
		
		logger.trace("Finished call to RestoreRequestHandlerLocalQueue.queueRequest for restoreRequest: %s", restoreRequest);
		
	}

	@Override
	public BackupRestoreRequest retreiveNextRestoreRequest() throws Exception {
		logger.trace("Called RestoreRequestHandlerLocalQueue.retreiveNextRestoreRequest");
		
		File queueDir = new File(this.localRestoreRequestQueueDir);
		BackupRestoreRequest retVal = null;
				
		if(false == queueDir.exists()) {
			logger.info(String.format("Restore request queue directory: %s does not exist, returning null from BackupRestoreRequest.retreiveNextRestoreRequest"));
		} else {						
			ArrayList<File> filteredList = new ArrayList<File>();			
						
			for(File file : queueDir.listFiles()) {
				if(file.getName().toLowerCase().startsWith("restorerequest")) {
					
					logger.info(String.format("Found restore request file %s in queue directory: %s", file.getName(), queueDir));
					
					filteredList.add(file);					
				}
			}
			
			//Note: Not checking for correct format, only files exist
			if( 0 == filteredList.size()) {
				logger.info(String.format("No restore requests found in queue directory: %s", queueDir));
			} else {			
				filteredList.sort(new Comparator<File>() {			    
					@Override
					public int compare(File f1, File f2) {			
						return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
					} });
				
				//Only take first file after sort
				retVal = recreateRestoreRequestFromFile(filteredList.get(0));
				
				filteredList.get(0).delete();
					
				logger.info(String.format("Returning restore request: %s from queue retrieve", retVal.getRequestID()));										
			}
		}
		
		logger.trace(String.format("Returning restore request from queue retrieve: %s", ((retVal == null) ? "null" : retVal.toString())));
		
		return retVal;
	}
	
	private BackupRestoreRequest recreateRestoreRequestFromFile(File requestFile) throws Exception {
		
		FileInputStream fis = null;
		BackupRestoreRequest restoreRequest = null;
		
		try {			
						
			fis = new FileInputStream(requestFile);
				
			JAXBContext jc = JAXBContext.newInstance( BackupRestoreRequest.class );
			Unmarshaller um = jc.createUnmarshaller();

			restoreRequest = (BackupRestoreRequest)um.unmarshal(fis);
			
		}finally {
			if( null != fis) {
				fis.close();				
			}		
		}		
		
		return restoreRequest;		
	}

}
