package com.nearskysolutions.cloudbackup.queue;

import java.util.Date;
import java.util.UUID;

public class ClientUpdateMessage {

	public enum ClientUpdateMessageType {	
		FileTracker,
		FilePacket,
		FileRestore,
		Unknown
	}
	
	public enum ClientUpdateMessageFormat {	
		JSON,
		XML
	}
	
	private UUID messageID;
	private Date dateTimeCreated;
	private ClientUpdateMessageType messageType;
	private ClientUpdateMessageFormat messageFormat;
	private String messageBody;
	
	public ClientUpdateMessage(Date dateTimeCreated, 
							   ClientUpdateMessageType messageType,
							   ClientUpdateMessageFormat messageFormat, 
							   String messageBody) {
	
			this.messageID = UUID.randomUUID();
			this.dateTimeCreated = dateTimeCreated;
			this.messageType = messageType;
			this.messageFormat = messageFormat;
			this.messageBody = messageBody;
	}
	
	public UUID getMessageID() {
		return messageID;
	}
	
	public Date getDateTimeCreated() {
		return dateTimeCreated;
	}
		
	public ClientUpdateMessageType getMessageType() {
		return messageType;
	}
		
	public ClientUpdateMessageFormat getMessageFormat() {
		return messageFormat;
	}
		
	public String getMessageBody() {
		return messageBody;
	}	
}
