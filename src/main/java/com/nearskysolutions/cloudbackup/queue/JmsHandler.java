package com.nearskysolutions.cloudbackup.queue;

import javax.jms.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
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
					//Back off to so reconnect can be attemted
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
    public JmsListenerContainerFactory<?> jmsFactory(ConnectionFactory connectionFactory,
                                                    DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
              
        // This provides all boot's default to this factory, including the message converter
        configurer.configure(factory, connectionFactory);
        // You could still override some of Boot's default if necessary.
        return factory;
    }
    
    @Bean // Serialize message content to json using TextMessage
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}
