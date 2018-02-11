package com.nearskysolutions.cloudbackup.queue;

import java.io.UnsupportedEncodingException;

import javax.jms.ConnectionFactory;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.JmsException;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.util.JsonConverter;

@Component
@EnableJms
public class JmsHandler {

	static Logger logger = LoggerFactory.getLogger(JmsHandler.class);
	
	@Autowired
	private JmsTemplate jmsTemplate;
		
	public void sendJsonJmsMessage(String destination, String messageObjectKey, Object obj, boolean bUseRetry) throws Exception {		
		if( null == destination ) {
			throw new Exception("destination argument can't be null");
		}
		
		if( null == obj ) {
			throw new Exception("obj argument can't be null");
		}
				
		String queueSelection = String.format("%s.%s", 
									destination, ((null != messageObjectKey && messageObjectKey.length() > 0) ? messageObjectKey.substring(0,1) : "x"));
		
		String message = JsonConverter.ConvertObjectToJson(obj);
		int numRetries = (bUseRetry ? 5 : 0);
		int retryMultiplier = 1;
		
		logger.info(String.format("Sending object of type %s to queueSelection=%s", obj.getClass().getName(), queueSelection));
		
		do
		{
			try {
				
				this.jmsTemplate.convertAndSend(queueSelection, message);
				
				numRetries = 0;
				
			} catch (JmsException jmsEx) {
				
				logger.info(String.format("JMS error: %s while trying to send message of type %s to queueSelection=%s, backing off for retry", 
										 	jmsEx.getMessage(),	obj.getClass().getName(), queueSelection));
				
				Thread.sleep(60000 * retryMultiplier++);				
			}
		} while(--numRetries >= 0);
	}
	
    @Bean
    public ConnectionFactory jmsConnectionFactory(JmsQpidConfiguration qpidConfig) throws UnsupportedEncodingException {
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(qpidConfig.getUrlString());
        jmsConnectionFactory.setUsername(qpidConfig.getServiceUsername());
        jmsConnectionFactory.setPassword(qpidConfig.getServicePassword());
        jmsConnectionFactory.setClientID(qpidConfig.getClientId());             
        jmsConnectionFactory.setReceiveLocalOnly(true);        
        
        return new CachingConnectionFactory(jmsConnectionFactory);
    }
    
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory jmsConnectionFactory) {
    	return new JmsTemplate(jmsConnectionFactory);
    }
    
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory returnValue = new DefaultJmsListenerContainerFactory();        
        returnValue.setConnectionFactory(connectionFactory);

        return returnValue;
    }    

}
