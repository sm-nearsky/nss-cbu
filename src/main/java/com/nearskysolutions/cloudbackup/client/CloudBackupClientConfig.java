package com.nearskysolutions.cloudbackup.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudBackupClient")
public class CloudBackupClientConfig {

	private String testValue;

	public String getTestValue() {
		return testValue;
	}

	public void setTestValue(String testValue) {
		this.testValue = testValue;
	}
	
	
}
