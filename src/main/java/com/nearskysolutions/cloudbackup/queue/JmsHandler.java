package com.nearskysolutions.cloudbackup.queue;

import java.io.UnsupportedEncodingException;

import javax.jms.ConnectionFactory;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.UncategorizedJmsException;
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
			
	public JmsHandler() {
		
	}
		
	public void sendJsonJmsMessage(String destination, Object obj, boolean bUseRetry) throws Exception {		
		if( null == destination ) {
			throw new Exception("destination argument can't be null");
		}
		
		if( null == obj ) {
			throw new Exception("obj argument can't be null");
		}
				
		String message = JsonConverter.ConvertObjectToJson(obj);
		int numRetries = (bUseRetry ? 3 : 0);
				
		logger.info(String.format("Sending object of type %s to destination=%s", obj.getClass().getName(), destination));
		
		do
		{
			try {
				
				this.jmsTemplate.convertAndSend(destination, message);
				
				numRetries = 0;
				
			} catch (UncategorizedJmsException jmsEx) {
				if( -1 != jmsEx.getMessage().indexOf("Address already in use") ) {
					//Back off to so reconnect can be attempted
					logger.info(String.format("Connection failed while trying to send message of type %s to destination=%s, backing off for retry", 
												obj.getClass().getName(), destination));
					
					Thread.sleep(500);
				} else {
					//Can't recover, throw the exception
					throw jmsEx;
				}
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
        JmsTemplate returnValue = new JmsTemplate();
        returnValue.setConnectionFactory(jmsConnectionFactory);
        return returnValue;
    }
    
    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory returnValue = new DefaultJmsListenerContainerFactory();
        returnValue.setConnectionFactory(connectionFactory);
        return returnValue;
    }
    
//    @Bean
//    public JmsListenerContainerFactory topicJmsListenerContainerFactory(ConnectionFactory connectionFactory) {
//        DefaultJmsListenerContainerFactory returnValue = new DefaultJmsListenerContainerFactory();
//        returnValue.setConnectionFactory(connectionFactory);
//        returnValue.setSubscriptionDurable(Boolean.TRUE);
//        return returnValue;
//    }    
}
