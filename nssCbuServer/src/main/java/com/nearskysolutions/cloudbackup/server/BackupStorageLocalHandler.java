package com.nearskysolutions.cloudbackup.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;

@Component(value="LocalBackupStorage")
public class BackupStorageLocalHandler extends BackupStorageHandlerBase {
	
	Logger logger = LoggerFactory.getLogger(BackupStorageLocalHandler.class);
		
	@Value( "${com.nearskysolutions.cloudbackup.localstore.fileStorageRootDir}" )
	private String fileStorageRootDir;
	
	@Value( "${com.nearskysolutions.cloudbackup.localstore.fileStorageRestoreDir}" )
	private String fileStorageRestoreDir;
	
	private File systemTempDir;
	
	public BackupStorageLocalHandler() throws Exception {
		String sysProp = System.getProperty("java.io.tmpdir");
		
		if( null == sysProp ) {
			throw new Exception("Missing system property for java.io.tmpdir");
		}
		
		systemTempDir = new File(sysProp);
		
		if(false == systemTempDir.exists()) {
			throw new Exception(String.format("System temp directory '%s' doesn't exist.", sysProp));
		}
	}
						
	@Override
	protected void deleteFinalTrackerFile(BackupFileTracker tracker) throws Exception {
		File finalZipFile = getFinalZipFile(tracker);				
		
		if(finalZipFile.exists()) {			
			finalZipFile.delete();
		}
	}

	@Override
	protected void handleFirstPacket(BackupFileTracker tracker) throws Exception {
		
	}

	@Override
	protected void preProcessPacketFile(BackupFileTracker tracker, BackupFileDataPacket packet) throws Exception {
		File tmpFile = getPacketFile(tracker, packet.getPacketNumber());
		File fileDir = tmpFile.getParentFile();
		
		if(fileDir.exists())  {				
			
			logger.info(String.format("Starting new stored file for tracker ID: %s, packet #: %d, client ID: %s", 
							tracker.getBackupFileTrackerID().toString(), packet.getPacketNumber(), tracker.getClientID().toString()));
													
			if(tmpFile.exists()) {
				tmpFile.delete();
			}
			
		} else {
			
			logger.info(String.format("Creating file directory %s for tracker ID: %s", fileDir.getAbsolutePath(), tracker.getClientID().toString()));
			
			//Create tracker dir with all parents when needed			
			fileDir.mkdirs();								
		}		
	}

	@Override
	protected InputStream getPacketFileInputStream(BackupFileTracker tracker, int packetNum) throws Exception {
		return new FileInputStream(getPacketFile(tracker, packetNum));
	}
	
	@Override
	protected OutputStream getPacketFileOutputStream(BackupFileTracker tracker, int packetNum) throws Exception {
		File tmpFile = getPacketFile(tracker, packetNum);
		
		return new FileOutputStream(tmpFile);
	}

	@Override
	protected OutputStream getFinalFileOutputStream(BackupFileTracker tracker) throws Exception {
		return new FileOutputStream(getFinalZipFile(tracker));
	}

	@Override
	protected InputStream getFinalFileInputStream(BackupFileTracker tracker) throws Exception {
		return new FileInputStream(getFinalZipFile(tracker));
	}

	@Override
	protected boolean checkTempFileExists(BackupFileTracker tracker) throws Exception {
		File tempFile = getTempFile(tracker);
		
		return tempFile.exists();
	}
	
	@Override
	protected InputStream getTempFileInputStream(BackupFileTracker tracker) throws Exception {
		File tempFile = getTempFile(tracker);
		
		return new FileInputStream(tempFile);
	}

	@Override
	protected OutputStream getTempFileOutputStream(BackupFileTracker tracker) throws Exception {
		File tempFile = getTempFile(tracker);
		
		return new FileOutputStream(tempFile);		
	}
	
	@Override
	protected void deleteTempFile(BackupFileTracker tracker) throws Exception {
		File tempFile = getTempFile(tracker);
		
		if( tempFile.exists() ) {
			tempFile.delete();
		}
	}
	
	@Override
	protected void deletePacketFile(BackupFileTracker tracker, int packetNum) throws Exception {
		File packetFile = getPacketFile(tracker, packetNum);
		
		if( packetFile.exists() ) {
			packetFile.delete();
		}	
	}
	
	@Override 
	protected boolean checkPacketFileExists(BackupFileTracker tracker, int packetNum) throws Exception {
		File packetFile = getPacketFile(tracker, packetNum);
		
		return ( packetFile.exists() );
	}

	
	@Override
	protected boolean checkTrackerFileExists(BackupFileTracker tracker) throws Exception {
		File finalZipFile = getFinalZipFile(tracker);				
		
		return finalZipFile.exists();
	}

	@Override
	protected void deleteFinalRestoreFile(BackupRestoreRequest restoreRequest) throws Exception {
		File restoreFile = getFinalRestoreFile(restoreRequest);
				
		if( restoreFile.exists() ) {
			restoreFile.delete();
		}
	}
	
	@Override
	protected OutputStream getFinalRestoreFileOutputStream(BackupRestoreRequest restoreRequest) throws Exception {
		File restoreFile = getFinalRestoreFile(restoreRequest);
		
		return new FileOutputStream(restoreFile);
	}

	@Override
	protected String getUrlForFinalRestoreFile(BackupRestoreRequest restoreRequest) throws Exception {
		File restoreFile = getFinalRestoreFile(restoreRequest);
		
		return restoreFile.getAbsolutePath();
	}

	@Override
	protected long getFinalRestoreFileTotalSize(BackupRestoreRequest restoreRequest) throws Exception {
		File restoreFile = getFinalRestoreFile(restoreRequest);
		
		return ( restoreFile.exists() ? restoreFile.length() : 0);		
	}

	
	private File getFinalZipFile(BackupFileTracker tracker) {
		return new File(String.format("%s%s%s.zip", getTrackerDirectory(tracker).getAbsolutePath(), File.separator, tracker.getBackupFileTrackerID().toString()));
	}
	
	private File getTrackerDirectory(BackupFileTracker tracker) {
		
		File fileDir = new File(String.format("%s%s%s%s%s", 
								this.fileStorageRootDir,
								File.separator,
								tracker.getClientID().toString(),
								File.separator,
								tracker.getBackupFileTrackerID().toString().substring(0, 2)));
		return fileDir;
	}

	private File getPacketFile(BackupFileTracker tracker, int packetNumber) {
		
		File fileDir = getTrackerDirectory(tracker);
		
		File packetFile = new File(String.format("%s%s%s.%d",
								fileDir.getAbsolutePath(),
								File.separator,
								tracker.getBackupFileTrackerID().toString(),
								packetNumber));
		
		return packetFile;
	}
	
	private File getFinalRestoreFile(BackupRestoreRequest restoreRequest) {
		String finalRestoreFileName = String.format("%s%s%s.zip", this.fileStorageRestoreDir, File.separator, restoreRequest.getRequestID().toString());
		
		return new File(finalRestoreFileName);		
	}
	
	private File getTempFile(BackupFileTracker tracker) {
		File finalFile = getFinalZipFile(tracker);
		
		return new File(String.format("%s.tmp", 
									   finalFile.getAbsolutePath().substring(0, finalFile.getAbsolutePath().length()-4)));
	}

	
	//TODO Handle file attributes
	/*
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
		
	}*/
	 
}
