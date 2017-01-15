package com.nearskysolutions.cloudbackup.client;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
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
	
	public void RunTest1() {
		
		String outputFile = "C:\\tmp\\testOutput.gz";
		
		List<String> packetFileList = new ArrayList<String>();
		packetFileList.add("C:\\tmp\\nssCbuPacketQueue\\batch4_packet1_number1.xml");
		packetFileList.add("C:\\tmp\\nssCbuPacketQueue\\batch4_packet2_number2.xml");
		
		try {
			this.recreatePacketFileData(packetFileList, outputFile);
		} catch (Exception e) {			
			e.printStackTrace();
		}
		
	}
	
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
//			List<String> directoryIncludes = new ArrayList<String>();
//			directoryIncludes.add("C:\\tmp\\backupfileset1");
//			directoryIncludes.add("C:\\tmp\\backupfileset2");
//			client.setDirectoryIncludes(directoryIncludes);
//			
//			clientSvc.updateBackupClient(client);
			
			logger.info("Got client with " + client.getDirectoryIncludes().size() + " includes");
			
			List<BackupFileTracker> lstTrackers = new ArrayList<BackupFileTracker>();
			
			for(String directory : client.getDirectoryIncludes()) {			
				lstTrackers.addAll(fileHandlerSvc.updateFileTrackerListing(client.getClientID(), directory, "TestRepoType", "TestRepoLoc", "TestRepoKey"));
			}	
					
			BackupFileDataBatch fileBatch = null;
			int trackerCount = 0;
			
			for(BackupFileTracker tracker : lstTrackers) {
				if( tracker.isFileChanged() ) {
					if( fileBatch == null ) {
						fileBatch = this.dataSvc.addBackupFileDataBatch(new BackupFileDataBatch(client.getClientID()));
					}
					
					fileHandlerSvc.createPacketsForFile(fileBatch, tracker);
					
					trackerCount += 1;
				}
			}								
			
			if( fileBatch == null ) {
				logger.info("No changes detected, no batch sent");
			} else {
				
				fileHandlerSvc.sendBatchToProcessingQueue(fileBatch);
				logger.info("Batch sent with tracker count: " + trackerCount);
			}		
			
		} catch (Exception e) {			
			e.printStackTrace();
		}     

//		try
//		{
//			File f = new File("/C:/tmp/nssCbuPacketStaging/bb1e767a-ce71-4511-a765-97b459d528b0.0");
//			FileInputStream fis = new FileInputStream(f);
//			ByteArrayOutputStream bos = new ByteArrayOutputStream();
//			
//			int b = fis.read();
//			
//			while( b >= 0 ) {
//				bos.write(b);
//				
//				b = fis.read();
//			}
//			 
//			fis.close();
//			fis = null;
//				
//			byte[] bytes = bos.toByteArray();
//			
//			bytes = Base64Utils.decode(bytes);
//			
//			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
//			GZIPInputStream gis = new GZIPInputStream(bis);
//			
//			try (				    
//				    InputStream ungzippedResponse = new GZIPInputStream(bis);
//				    Reader reader = new InputStreamReader(ungzippedResponse);
//				    Writer writer = new StringWriter();
//				) {
//				    char[] buffer = new char[10240];
//				    for (int length = 0; (length = reader.read(buffer)) > 0;) {
//				        writer.write(buffer, 0, length);
//				    }
//				    System.out.println(writer.toString());
//				}
//
//		} catch (Exception e) {			
//			e.printStackTrace();
//		}        

	}
	
	private void recreatePacketFileData(List<String> packetFileList, String outputFile) throws Exception {
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		
		try {
			
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			
			for(String filePath : packetFileList) {
			
				logger.info("Reading file: " + filePath);
				
				fis = new FileInputStream(filePath);
				
				JAXBContext jc = JAXBContext.newInstance( BackupFileDataPacket.class );
				Unmarshaller um = jc.createUnmarshaller();
				BackupFileDataPacket packet = (BackupFileDataPacket)um.unmarshal(fis);
				
				String packetData = packet.getFileData();
				
				byteStream.write(Base64.getDecoder().decode(packetData));				
			}			
									
			fos = new FileOutputStream(outputFile);
				
			fos.write(byteStream.toByteArray());
			
		}finally {
			if( null != fis) {
				fis.close();				
			}
			
			if( null != fos) {
				fos.close();				
			}
		}
		
		
	}
}
