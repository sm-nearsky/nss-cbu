package com.nearskysolutions.cloudbackup.queue;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.queue.ClientUpdateMessage.ClientUpdateMessageFormat;
import com.nearskysolutions.cloudbackup.queue.ClientUpdateMessage.ClientUpdateMessageType;
import com.nearskysolutions.cloudbackup.util.JsonConverter;

@Component
public class ClientUpdateHandlerQueue {
	
	Logger logger = LoggerFactory.getLogger(ClientUpdateHandlerQueue.class);
	
	@Autowired
	private JmsHandler jmsHandler;
			
	@Value("${com.nearskysolutions.cloudbackup.queue.clientUpdateQueueName}")
	private String queueName;
	
	private String clientUpdateQueueName = null;
	
	public ClientUpdateHandlerQueue(){
			
	}
	
	public String getClientUpdateQueueName() throws Exception { 
		if( null == this.clientUpdateQueueName ) {						
						
			if( null == this.queueName || this.queueName.isEmpty() ) {
				throw new Exception("Client update queue queue name property missing or empty");
			}
			
			this.clientUpdateQueueName = queueName;
			
			logger.info(String.format("Setting client update queue name: %s", this.clientUpdateQueueName));
		}
		
		return this.clientUpdateQueueName;
	}
		
	public void sendFileTrackerUpdate(BackupFileTracker fileTracker) throws Exception {
		logger.trace("In ClientUpdateHandlerLocalQueue.sendFileTrackerUpdate(BackupFileTracker fileTracker)");
		
		if(null == fileTracker) {
			throw new Exception("fileTracker can't be null");
		}
		
		sendJmsMessageToQueue(ClientUpdateMessageType.FileTracker, 
								(fileTracker.getBackupFileTrackerID() != null ? fileTracker.getBackupFileTrackerID().toString() : null), 
								fileTracker);
		
		logger.trace("Completed ClientUpdateHandlerLocalQueue.sendFileTrackerUpdate(BackupFileTracker fileTracker)");
	}
	
	public void sendBackupFilePacket(BackupFileDataPacket filePacket) throws Exception {
		
		logger.trace("In ClientUpdateHandlerLocalQueue.sendFileTrackerUpdate(BackupFileTracker fileTracker)");
		
		if(null == filePacket) {
			throw new Exception("filePacket can't be null");
		}
		
		sendJmsMessageToQueue(ClientUpdateMessageType.FilePacket, filePacket.getFileTrackerID().toString(), filePacket);
			
		logger.trace("Completed ClientUpdateHandlerLocalQueue.sendFileTrackerUpdate(BackupFileTracker fileTracker)");
		
	}
	
	public void sendBackupRestoreRequest(BackupRestoreRequest restoreRequest) throws Exception {
		logger.trace("In ClientUpdateHandlerLocalQueue.sendBackupRestoreRequest(BackupRestoreRequest restoreRequest)");
		
		if(null == restoreRequest) {
			throw new Exception("restoreRequest can't be null");
		}
		
		sendJmsMessageToQueue(ClientUpdateMessageType.FileRestore, restoreRequest.getRequestID().toString(), restoreRequest);
				
		logger.trace("Completed ClientUpdateHandlerLocalQueue.sendBackupRestoreRequest(BackupRestoreRequest restoreRequest)");
		
	}

	private void sendJmsMessageToQueue(ClientUpdateMessageType messageType, String messageObjectKey, Object messageObj) throws Exception {
	
		ClientUpdateMessage updateMessage = new ClientUpdateMessage(new Date(), 
																	messageType, 
																	ClientUpdateMessageFormat.JSON, 
																	JsonConverter.ConvertObjectToJson(messageObj)); 
			
		
		logger.info(String.format("Sending file tracker client update message with message ID: %s", 
									updateMessage.getMessageID().toString()));
		
		this.jmsHandler.sendJsonJmsMessage(this.getClientUpdateQueueName(), messageObjectKey, updateMessage, true);	
		
	}
}
