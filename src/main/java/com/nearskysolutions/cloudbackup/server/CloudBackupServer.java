package com.nearskysolutions.cloudbackup.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.FileHandlerService;

@SpringBootApplication
@EnableJpaRepositories("com.nearskysolutions.cloudbackup.data")
@EntityScan("com.nearskysolutions.cloudbackup.common")
public class CloudBackupServer  implements CommandLineRunner {
	
	Logger logger = LoggerFactory.getLogger(CloudBackupServer.class);
	
	@Autowired	
	private FileHandlerService fileHandlerSvc;
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	@Autowired 
	private CloudBackupServerConfig cbcConfig;
		    
	public void run(String... args) {
		
//		TestRunClass trc = appContext.getBean(TestRunClass.class);		
//		trc.RunTest();

		this.retrieveAndProcessBackupPackets();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupServer.class, args);
	}	
	
	private void retrieveAndProcessBackupPackets() {
										
		try {
		
			BackupFileDataBatch fileBatch = dataSvc.getDataBatchByBatchID(3L);
			
			List<BackupFileDataPacket> packetList = this.fileHandlerSvc.retrieveBatchFromProcessingQueue(fileBatch.getFileBatchID());
						
			while(packetList.size() > 0) {
				Long trackerID = packetList.get(packetList.size()-1).getFileTrackerID();
				List<BackupFileDataPacket> packetsForFile = new ArrayList<BackupFileDataPacket>();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				for(int i = packetList.size(); --i >= 0;) {					
					if( packetList.get(i).getFileTrackerID() == trackerID) {
						packetsForFile.add(packetList.get(i));
						packetList.remove(i);
					}					
				}	
				
				if( 1 == packetsForFile.size() && FileAction.Delete == packetsForFile.get(0).getFileAction() ) {
					
					File fileDir = new File(String.format("%s%s%s", 
							this.cbcConfig.getFileStorageRootDir(),
							File.separator,
							trackerID));

					if(fileDir.exists()) {					
						//Clear any files
						for(File file : fileDir.listFiles()) {
							file.delete();
						}
						
						//Delete tracker directory
						fileDir.delete();
					}
					
				} else {
					packetsForFile.sort(new Comparator<BackupFileDataPacket>() {			    
						@Override
						public int compare(BackupFileDataPacket p1, BackupFileDataPacket p2) {			
							return Integer.compare(p1.getPacketNumber(), p2.getPacketNumber());
						} });
									
					for(BackupFileDataPacket packet : packetsForFile) {
						baos.write(Base64.getDecoder().decode(packet.getFileData()));					
					}
					
					createStorageFile(baos, trackerID);
				}						
			}		
			
		} catch (Exception ex) {			
			logger.error("Unable to process data packets due to exception:", ex);
		}
	}

	private void createStorageFile(ByteArrayOutputStream baos, Long trackerID)
			throws FileNotFoundException, IOException {					
				
		File fileDir = new File(String.format("%s%s%s", 
												this.cbcConfig.getFileStorageRootDir(),
												File.separator,
												trackerID));

		if(fileDir.exists()) {
			
			//Replace file if tracker dir exists
			for(File file : fileDir.listFiles()) {
				file.delete();
			}
			
		} else {
			
			//Otherwise create tracker dir
			fileDir.mkdir();
		}
		
		File storageFile = new File(String.format("%s%s%s.gz",
													fileDir.getAbsolutePath(),
													File.separator,
													UUID.randomUUID()));		
		
		FileOutputStream fos = null;
		byte[] fileBytes = baos.toByteArray();
		int bufferSize = 4096;
		int bytesRead = 0;
		int readSize;
		
		try {
			fos = new FileOutputStream(storageFile);
		
			while(bytesRead < baos.toByteArray().length) {
				readSize = Math.min(bufferSize, fileBytes.length - bytesRead);
				
				fos.write(fileBytes, bytesRead, readSize);
				
				bytesRead += readSize;
			}
			
			fos.flush();
			fos.close();
			
		} finally {
			if( null != fos ) {
				fos.close();
			}
		}
	}
}
