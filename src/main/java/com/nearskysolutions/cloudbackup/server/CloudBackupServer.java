package com.nearskysolutions.cloudbackup.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import javax.persistence.Column;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.nearskysolutions.cloudbackup.common.BackupFileAttributes;
import com.nearskysolutions.cloudbackup.common.BackupFileDataBatch;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
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

		//this.retrieveAndProcessBackupPackets();
		
		this.recreateTrackerFiles();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CloudBackupServer.class, args);
	}	
	
	private void retrieveAndProcessBackupPackets() {
										
		try {
		
			//TODO Add client dir
			
			BackupFileDataBatch fileBatch = dataSvc.getDataBatchByBatchID(2L);
			
			List<BackupFileDataPacket> packetList = this.fileHandlerSvc.retrieveBatchFromProcessingQueue(fileBatch.getFileBatchID());
						
			while(packetList.size() > 0) {				
				Long trackerID = packetList.get(packetList.size()-1).getFileTrackerID();
				BackupFileTracker tracker = dataSvc.getTrackerByBackupFileTrackerID(trackerID);
				List<BackupFileDataPacket> packetsForFile = new ArrayList<BackupFileDataPacket>();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				for(int i = packetList.size(); --i >= 0;) {					
					if( packetList.get(i).getFileTrackerID().longValue() == trackerID.longValue()) {
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
									
					if( false == tracker.isDirectory() ) {
						for(BackupFileDataPacket packet : packetsForFile) {
							baos.write(Base64.getDecoder().decode(packet.getFileData()));					
						}
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
				
		File fileDir = new File(String.format("%s%s%d", 
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
	
	private void recreateTrackerFiles() {
		try {
			
			//TODO: Temp for testing
			String restoreRootDir = "/C:/tmp/nssCbuFileRestore";
			
			UUID clientID = UUID.fromString("57649898-ec95-48ab-a257-4bf7cbb971c9");
			
			List<BackupFileTracker> trackerList = this.dataSvc.getActiveBackupTrackersForClient(clientID);
						
			for(BackupFileTracker tracker : trackerList) {
				File trackerDir = new File(String.format("%s%s%d", 
						this.cbcConfig.getFileStorageRootDir(),
						File.separator,
						tracker.getBackupFileTrackerID()));
				
				if(false == trackerDir.exists()) {
					throw new Exception(String.format("Couldn't find directory for tracker ID: %d - %s",
										tracker.getBackupFileTrackerID(), trackerDir));
				}
				
				
				String restorePath;
				//TODO Make configurable
				int driveLetterIdx = tracker.getSourceDirectory().indexOf(":");
				
				if( 0 > driveLetterIdx ) {
					restorePath = tracker.getSourceDirectory();
				} else {
					restorePath = tracker.getSourceDirectory().substring(driveLetterIdx + 1);
				}
											
				File restoreFile;
				
				//restorePath is whole path for directories
				if( tracker.isDirectory() ) {
					restoreFile = new File(String.format("%s%s%s", 
							restoreRootDir, 
							File.separator, 
							restorePath));
				} else {
					//Not a directory, file name required
					restoreFile = new File(String.format("%s%s%s%s%s", 
														restoreRootDir, 
														File.separator, 
														restorePath, 
														File.separator, 
														tracker.getFileName()));
				}
				
				File[] dirFiles = trackerDir.listFiles(new FilenameFilter() {					
					@Override
					public boolean accept(File dir, String name) {						
						return name.toLowerCase().endsWith(".gz");
					}
				});
				
				if(1 != dirFiles.length) {
					throw new Exception(String.format("Invalid number of files found in storage directory: %s, expected 1 found %d",
														trackerDir, dirFiles.length));
				}
				
				if(false == restoreFile.getParentFile().exists()) {
					logger.info(String.format("Creating restore directory: %s", restoreFile.getParentFile().getAbsolutePath()));
					
					restoreFile.getParentFile().mkdirs();
				}
				
				if( tracker.isDirectory() ) {
					if( false == restoreFile.exists() ) {
						restoreFile.mkdir();		
						setFileAttributes(restoreFile, tracker.getFileAttributes());
					}
				} else {
					createFileFromGZip(dirFiles[0], restoreFile);
					
					setFileAttributes(restoreFile, tracker.getFileAttributes());
				}				
			}
			
		} catch (Exception ex) {			
			logger.error("Unable to recreate tracker files due to exception:", ex);
		}
	}

	private void createFileFromGZip(File sourceFile, File destFile) throws IOException {
		
		GZIPInputStream gis = null;
		FileOutputStream gzipOut = null;
		int byteCount;
		byte[] readBytes = new byte[1024];
		
		logger.info(String.format("Restoring compressed data to final file: %s", destFile.getAbsolutePath()));
		
		try
		{
			gis = new GZIPInputStream(new FileInputStream(sourceFile));
		
		    gzipOut = new FileOutputStream(destFile);
		    
		    while ((byteCount = gis.read(readBytes)) > 0) {
		    	gzipOut.write(readBytes, 0, byteCount);
		    }	    			        
		 
		} catch (IOException ex) {		
			throw ex;		
		} finally {			
			
			if(null != gis) {
				gis.close();
			}
			
			if(null != gzipOut) {
				gzipOut.close();
			}		
		}
	}
	
private void setFileAttributes(File restoreFile, BackupFileAttributes fileAttributes) throws IOException {
	    
	    //TODO Make configurable

		//Note: Not all saved attributes are used because some
		// 		are derived
	
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
	    				   "basic:lastModifiedTime", 
	    				   FileTime.fromMillis(fileAttributes.getFileModifiedDateTimeMillis()),
	    				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "basic:creationTime", 
				   FileTime.fromMillis(fileAttributes.getFileCreatedDateTimeMillis()),
				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "basic:lastAccessTime", 
				   FileTime.fromMillis(fileAttributes.getFileAccessDateTimeMillis()),
				   LinkOption.NOFOLLOW_LINKS);	    
	 	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "dos:archive", 
				   fileAttributes.isArchive(),
				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "dos:hidden", 
				   fileAttributes.isHidden(),
				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "dos:readonly", 
				   fileAttributes.isReadOnly(),
				   LinkOption.NOFOLLOW_LINKS);
	    
	    Files.setAttribute(Paths.get(restoreFile.getAbsolutePath()), 
				   "dos:system", 
				   fileAttributes.isSystem(),
				   LinkOption.NOFOLLOW_LINKS);
		
	}
}
