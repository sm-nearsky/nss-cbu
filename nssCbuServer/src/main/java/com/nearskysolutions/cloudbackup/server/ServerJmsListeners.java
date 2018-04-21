package com.nearskysolutions.cloudbackup.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.queue.ClientUpdateMessage;
import com.nearskysolutions.cloudbackup.util.JsonConverter;


@Component
public class ServerJmsListeners {

	Logger logger = LoggerFactory.getLogger(ServerJmsListeners.class);
	
	@Autowired
	private CloudBackupServer cloudBackupServer;
	
	@JmsListener(destination = "nssCbuClientUpdates", concurrency="1-10")
	public void receiveMessage(String message) {

		logger.trace("In CloudBackupServer.receiveMessage(String message)");
								
		ClientUpdateMessage updateMessage = (ClientUpdateMessage)JsonConverter.ConvertJsonToObject(message, ClientUpdateMessage.class);
        
		logger.info(String.format("Cloud backup message received from queue, message ID: %s and message type: %s",
									updateMessage.getMessageID().toString(), updateMessage.getMessageType().toString()));
		
		try {
			switch(updateMessage.getMessageType()) {
				case FileTracker:
					BackupFileTracker tracker = (BackupFileTracker)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupFileTracker.class);        	
		        	logger.info(String.format("Processing add or update for backup file tracker ID: %s", tracker.getBackupFileTrackerID()));
		        	
		        	this.cloudBackupServer.processTrackerUpdate(tracker);
		        	
		        break;	
				case FilePacket:        	
		        	BackupFileDataPacket packet = (BackupFileDataPacket)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupFileDataPacket.class);        	
		        	logger.info(String.format("Processing backup file packet with ID: %s", packet.getDataPacketID().toString()));
		        	
		        	this.cloudBackupServer.processPacketUpdate(packet);
		        	
		        break;
				case FileRestore:        	
		        	BackupRestoreRequest restoreRequest = (BackupRestoreRequest)JsonConverter.ConvertJsonToObject(updateMessage.getMessageBody(), BackupRestoreRequest.class);        	
		        	logger.info(String.format("Processing restore request ID: %s", restoreRequest.getRequestID().toString()));
		        	
		        	this.cloudBackupServer.processRestoreRequest(restoreRequest);			        		        	        	        	
		        	break;        	
		        default:
		        	throw new Exception(String.format("Unknown message update type: %s for messsage ID: %s", 
		        			updateMessage.getMessageType(), updateMessage.getMessageID()));	        	
			}				
									
		} catch(Exception ex) {
			logger.error("Error in message handling", ex);
		}
		
		logger.trace("Completed CloudBackupServer.receiveMessage(String message)");
    }

	@JmsListener(destination = "nssCbuClientUpdates/$DeadLetterQueue", concurrency="1-5")
	public void receiveDLQMessage(String message) {
		logger.trace(String.format("Discarding dead letter message: %s", message.substring(0, Math.min(message.length(), 250))));		
	}
	
}
