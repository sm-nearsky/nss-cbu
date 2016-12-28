package com.nearskysolutions.cloudbackup.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import com.nearskysolutions.cloudbackup.common.BackupFileAttributes;
import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
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
//			logger.info("Got client with " + client.getDirectoryIncludes().size() + " includes");
//			
//			for(String dir : client.getDirectoryIncludes()) {
//				logger.info("Scanning for dir: " + dir);
//				List<File> files = fileHandlerSvc.scanFilesForDirectory(dir);
//								
//				for(File f : files) {
//					if( f.getAbsolutePath().indexOf("pom.xml") > 0 || f.getAbsolutePath().indexOf("zerotest.txt") > 0) {
////						fileHandlerSvc.storePacketsForFile(f);
//						
//						//TODO make this configurable in final form
//						DosFileAttributes  attr = (DosFileAttributes)Files.readAttributes(Paths.get(f.getAbsolutePath()), DosFileAttributes .class); 
//													
//						BackupFileTracker bft = new BackupFileTracker(client.getClientID(),
//								"test repo type",
//								"test repo loc",
//								"test repo key",
//								f.getAbsolutePath()
//								); 
//						
//						dataSvc.addBackupFileTracker(bft);
//					}
//					logger.info(f.getAbsolutePath() + " " + f.isDirectory());
//				}				
//				
//				logger.info("Total files: " + files.size());
//			}
			
			
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
}
