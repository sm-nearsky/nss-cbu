package com.nearskysolutions.cloudbackup.test;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudBackupClient")
@ComponentScan(basePackages="com.nearskysolutions.cloudbackup.client,com.nearskysolutions.cloudbackup.services")
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupClientTestConfig {

	private String testValue;

	public String getTestValue() {
		return testValue;
	}

	public void setTestValue(String testValue) {
		this.testValue = testValue;
	}
	
	
}
