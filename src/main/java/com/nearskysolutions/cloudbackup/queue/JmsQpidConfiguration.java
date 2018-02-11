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
	
	@Value("${com.nearskysolutions.cloudbackup.queue.qpid.prefetchCount}")
	private int prefetchCount;
	

	@Value("${com.nearskysolutions.cloudbackup.queue.qpid.idleTimeout}")
	private int idleTimeout;
	
	public String getClientId() {
		return this.amqpClientId;
	}
	
	public String getServiceUsername() {
		return serviceUsername;
	}
	    
	public String getServicePassword() {
		return servicePassword;
	}
	
	public int getPrefetchCount() {
		return prefetchCount;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}
	
	public String getUrlString() throws UnsupportedEncodingException {
		StringBuffer sb = new StringBuffer();
		sb.append("amqps://");
		sb.append(this.amqpHost);
		
		int paramCount = 0;
//		
//		if(null != this.prefetchCount && 0 != this.prefetchCount.trim().length()) {
			sb.append("?jms.prefetchPolicy.all=");
			sb.append(this.prefetchCount);
			
			paramCount += 1;
//		}
//		
//		if(null != this.idleTimeout && 0 != this.idleTimeout.trim().length()) {
			
			if( 0 == paramCount ) {
				sb.append("?");
			} else {
				sb.append("&");
			}
			
			sb.append("amqp.idleTimeout=");
			sb.append(this.idleTimeout);
			
			paramCount += 1;
//		}
		
		return sb.toString();
	}
}