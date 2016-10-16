package com.nearskysolutions.cloudbackup.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@Component
public class TestRunClass {

	Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);
	
	@Autowired	
	private BackupFileDataService packetSvc;
		
	public void RunTest() {
		
		
        
	}
}
