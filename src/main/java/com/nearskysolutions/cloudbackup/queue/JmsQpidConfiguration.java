package com.nearskysolutions.cloudbackup.queue;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JmsQpidConfiguration {
	
	@Value("{com.nearskysolutions.cloudbackup.queue.qpid.clientId}")
	private String amqpClientId;
	
	@Value("${com.nearskysolutions.cloudbackup.queue.qpid.amqpHost}")
	private String amqpHost;

	@Value("${com.nearskysolutions.cloudbackup.queue.qpid.serviceUser}")
	private String serviceUsername;

	@Value("${com.nearskysolutions.cloudbackup.queue.qpid.servicePass}")
	private String servicePassword;
	
	public String getClientId() {
		return this.amqpClientId;
	}
	
	public String getServiceUsername() {
		return serviceUsername;
	}
	    
	public String getServicePassword() {
		return servicePassword;
	}
	    
	public String getUrlString() throws UnsupportedEncodingException {
		return String.format("amqps://%1s?amqp.idleTimeout=3600000", this.amqpHost);
	}
}