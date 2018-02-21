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
			
	private void processMessage(String message) {

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

	@JmsListener(destination = "nssCbuClientUpdates.0", concurrency="1-1")
	public void receiveMessage0(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.0 channel"));
		
		this.processMessage(message);				
	}
		
	@JmsListener(destination = "nssCbuClientUpdates.1", concurrency="1-1")
	public void receiveMessage1(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.1 channel"));
		
		this.processMessage(message);				
	}
		
	@JmsListener(destination = "nssCbuClientUpdates.2", concurrency="1-1")
	public void receiveMessage2(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.2 channel"));
		
		this.processMessage(message);				
	}
		 
	@JmsListener(destination = "nssCbuClientUpdates.3", concurrency="1-1")
	public void receiveMessage3(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.3 channel"));
		
		this.processMessage(message);				
	}
			
	@JmsListener(destination = "nssCbuClientUpdates.4", concurrency="1-1")
	public void receiveMessage4(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.4 channel"));
		
		this.processMessage(message);				
	}
		
	@JmsListener(destination = "nssCbuClientUpdates.5", concurrency="1-1")
	public void receiveMessage5(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.5 channel"));
		
		this.processMessage(message);				
	}
		
	@JmsListener(destination = "nssCbuClientUpdates.6", concurrency="1-1")
	public void receiveMessage6(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.6 channel"));
		
		this.processMessage(message);				
	}
		
	@JmsListener(destination = "nssCbuClientUpdates.7", concurrency="1-1")
	public void receiveMessage7(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.7 channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.8", concurrency="1-1")
	public void receiveMessage8(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.8 channel"));
		
		this.processMessage(message);				
	}
			
	@JmsListener(destination = "nssCbuClientUpdates.9", concurrency="1-1")
	public void receiveMessage9(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.9 channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.a", concurrency="1-1")
	public void receiveMessageA(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.a channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.b", concurrency="1-1")
	public void receiveMessageB(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.b channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.c", concurrency="1-1")
	public void receiveMessageC(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.c channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.d", concurrency="1-1")
	public void receiveMessageD(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.d channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.e", concurrency="1-1")
	public void receiveMessageE(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.e channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.f", concurrency="1-1")
	public void receiveMessageF(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.f channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.x", concurrency="1-1")
	public void receiveMessageX(String message) {
		logger.trace(String.format("Received message on nssCbuClientUpdates.x channel"));
		
		this.processMessage(message);				
	}
	
	@JmsListener(destination = "nssCbuClientUpdates.0/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.1/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.2/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.3/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.4/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.5/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.6/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.7/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.8/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.9/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.a/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.b/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.c/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.d/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.e/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.f/$DeadLetterQueue", concurrency="1-5")
	@JmsListener(destination = "nssCbuClientUpdates.x/$DeadLetterQueue", concurrency="1-5")
	public void receiveDLQMessageA(String message) {
		logger.trace(String.format("Discarding dead letter message: %s", message.substring(0, Math.min(message.length(), 250))));		
	}
}
