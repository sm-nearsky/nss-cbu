package com.nearskysolutions.cloudbackup.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.client.CloudBackupClientConfig;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.data.BackupFileTrackerRepository;

@Component 
public class FileHandlerServiceImpl implements FileHandlerService {

	Logger logger = LoggerFactory.getLogger(FileHandlerServiceImpl.class);

	@Autowired
	CloudBackupClientConfig clientConfig;
	
	@Autowired
	BackupFileTrackerRepository trackerRepo;
		
	@Override 
	public void updateFileTrackerListing(UUID clientID, String rootDir) throws IOException {
				
		if( null == rootDir ) {
			logger.error("Null root directory argument passed to FileHandlerServiceImpl.updateFileTrackerListing");
			throw new NullPointerException("Root directory name can't be null");
		}
		
		List<BackupFileTracker> trackerFileList = new ArrayList<BackupFileTracker>();
		
		for(BackupFileTracker tracker : trackerRepo.findByClientID(clientID)) {
			if(tracker.getSourceDirectory().equalsIgnoreCase(rootDir)) {
				trackerFileList.add(tracker);
			}			
		}
		
		List<File> currentFileList = scanFilesForDirectory(rootDir);
		
		boolean fileFound;
		
		for(BackupFileTracker tracker : trackerFileList) {		
			fileFound = false;
			
			for(File currentFile : currentFileList) {
				
				fileFound = currentFile.getAbsolutePath().equalsIgnoreCase(tracker.getFullPath());
				
				if( fileFound ) {
					if( false == tracker.equalsFile(currentFile) ) {
						logger.info(String.format("Found changes for file: %s", tracker.getFullPath()));
						
						tracker.updateFileAttributes(currentFile);						
						tracker.setFileChanged(true);
					}
				
					break;
				}
			}
			
			if( false == fileFound ) {
				logger.info(String.format("Tracker file not found and marked for deletion: %s", tracker.getFullPath()));
				
				tracker.setFileChanged(true);
				tracker.setFileDeleted(true);
			}
			
			if(tracker.isFileChanged()) {
				trackerRepo.save(tracker);
			}			
		}
	}	
	
	private List<File> scanFilesForDirectory(String dir) throws IOException {
		List<File> files = new ArrayList<File>();
		
		if( null == dir ) {
			logger.error("Null directory argument passed to FileHandlerServiceImpl.scanFilesForDirectory");
			throw new NullPointerException("Directory name can't be null");
		} 

		File dirFile = new File(dir);
		
		if( false == dirFile.exists() || false == dirFile.isDirectory() ) {
			logger.error(String.format("File %s doesn't exist or is not a directory", dir));
			throw new IOException(String.format("File %s doesn't exist or is not a directory", dir));
		}
		
		collectFileList(dirFile, files);
		
		return files;
	}

	private void collectFileList(File dirFile, List<File> files) {

		logger.info(String.format("Scanning files for directory: %s", dirFile.getAbsolutePath()));
		
		List<File> childDirs = new ArrayList<File>();
		
		for(File f : dirFile.listFiles()) {
			if( f.isDirectory() ) {
				childDirs.add(f);
			} else {				
				logger.info(String.format("Adding file to scan list: %s", f.getAbsolutePath()));
				files.add(f);
			}
		}
		
		for(File dir : childDirs) {
			collectFileList(dir, files);
		}
	}

	@Override
	public void storePacketsForFile(File file) throws Exception {				
		if( null == file ) {
			throw new NullPointerException("File reference can't be null");
		} else if (false == file.exists() ) {
			throw new Exception(String.format("File %s doesn't exist", file.getAbsolutePath()));
		} else if (true == file.isDirectory() ) {
			throw new Exception(String.format("File %s is a directory", file.getAbsolutePath()));
		}
		
		logger.info(String.format("Storing packets for file: %s", file.getAbsolutePath()));
				
		FileInputStream fis = null;		
		FileOutputStream fos = null;
		int packetSize = clientConfig.getFilePacketSize();
		String baseFileName = String.format("%s%s%s", clientConfig.getPacketStagingDir(), File.separator, UUID.randomUUID());		
		File tempSaveFile = null;
		File finalSaveFile;
		int byteCount;
		int idx;
		byte[] readBytes;	
		byte[] writeBytes;
		int fileCount; 
		
		try {
			
			if(file.length() == 0) {
				tempSaveFile = new File(String.format("%s.0", baseFileName));
				
				logger.info(String.format("Saving zero byte file to: %s", tempSaveFile.getAbsolutePath()));
				
				fos = new FileOutputStream(tempSaveFile);
				fos.close();
				fos = null;
				
				finalSaveFile = new File(String.format("%s.gz",tempSaveFile.getAbsolutePath()));		        
				
				GZipFileOutput(tempSaveFile, finalSaveFile);
								
			} else {
				logger.info(String.format("Saving %d bytes before compress to: %s", file.length(), baseFileName));
				
				fis = new FileInputStream(file);				
				idx = 0;
				fileCount = 0;
				
				while(idx < file.length()) {
					tempSaveFile = new File(String.format("%s.%d", baseFileName, fileCount));
						
					readBytes = new byte[packetSize];
					byteCount = fis.read(readBytes);
					
					idx += byteCount;
					
					if(readBytes.length == byteCount) {
						writeBytes = readBytes;
					} else {
						writeBytes = new byte[byteCount];
						
						System.arraycopy(readBytes, 0, writeBytes, 0, byteCount);
					}
					
					fos = new FileOutputStream(tempSaveFile);
					fos.write(writeBytes);
					
					fos.close();
					fos = null;
					
					finalSaveFile = new File(String.format("%s.gz",tempSaveFile.getAbsolutePath()));		        
					
					GZipFileOutput(tempSaveFile, finalSaveFile);
					
					tempSaveFile.delete();
					
			        logger.info("Completed writing %d bytes to file: %s", finalSaveFile.length(), finalSaveFile.getAbsolutePath());
			        
					fileCount += 1;
				}
			}
			
			
			//TODO Create DB entries
			//TODO Add file attributes
			
		} catch (IOException ex) {
			
			throw ex;		
		} finally {
			if(null != fis) {
				fis.close();
			}
						
			if(null != fos) {
				fos.close();
			}
			
			if( null != tempSaveFile && tempSaveFile.exists() ) {
				try {
					tempSaveFile.delete();
				} catch (Exception e) {}
			}
		}
		
	}


	private void GZipFileOutput(File sourceFile, File destFile) throws IOException {
		
		GZIPOutputStream gos = null;
		FileInputStream gzipIn = null;
		int byteCount;
		byte[] readBytes = new byte[1024];
		
		logger.info(String.format("Saving compressed data to final file: %s", destFile.getAbsolutePath()));
		
		try
		{
			gos = new GZIPOutputStream(new FileOutputStream(destFile));
		
		    gzipIn = new FileInputStream(sourceFile);
		    
		    while ((byteCount = gzipIn.read(readBytes)) > 0) {
		    	gos.write(readBytes, 0, byteCount);
		    }
		    			        
			gos.finish();
			gos.close();
			gos = null;
		
			gzipIn.close();
			gzipIn = null;	
			
		} catch (IOException ex) {		
			throw ex;		
		} finally {
				
			
			if(null != gos) {
				gos.close();
			}
			
			if(null != gzipIn) {
				gzipIn.close();
			}		
		}
	}
	
}
