package com.nearskysolutions.cloudbackup.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.data.BackupFileDataPacketRepository;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.FileHandlerService;

@Component
public class TestRunClass {

	Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);
	
	@Autowired	
	private FileHandlerService fileHandlerSvc;
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	@Autowired 
	private BackupFileClientService clientSvc;
	
	public void RunTest() {
	
//		List<String> tmpIncs = new ArrayList<String>();
//		tmpIncs.add("/C:/tmp/brmstest");
//		tmpIncs.add("/C:/tmp/Machine-Learning-in-Java/MLJ-Chapter3");
//		
//		
//		BackupFileClient client1 = new BackupFileClient("client 1", "desc 1", "rep 1", "loc 1", "key 1", tmpIncs);
//		try {
//			clientSvc.addBackupClient(client1);
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		
		logger.info("Querying clients...");
		List<BackupFileClient> lstClients = clientSvc.getAllBackupClients();
		
		BackupFileClient client = lstClients.get(0);		
		
		try {
			logger.info("Got client with " + client.getDirectoryIncludes().size() + " includes");
			
			for(String dir : client.getDirectoryIncludes()) {
				logger.info("Scanning for dir: " + dir);
				List<File> files = fileHandlerSvc.scanFilesForDirectory(dir);
				
				for(File f : files) {
					logger.info(f.getAbsolutePath() + " " + f.isDirectory());
				}				
				
				logger.info("Total files: " + files.size());
			}
		} catch (Exception e) {			
			e.printStackTrace();
		}        
	}
}
