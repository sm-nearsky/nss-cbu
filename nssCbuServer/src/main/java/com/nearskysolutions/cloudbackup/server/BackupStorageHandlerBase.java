package com.nearskysolutions.cloudbackup.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.BackupRestoreRequestService;
import com.nearskysolutions.cloudbackup.util.FileZipUtils;
import com.nearskysolutions.cloudbackup.util.ZipEntryHelper;

public abstract class BackupStorageHandlerBase implements BackupStorageHandler {
	
	Logger logger = LoggerFactory.getLogger(BackupStorageHandlerBase.class);
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	@Autowired
	private BackupRestoreRequestService restoreSvc;
	
	@Autowired
	CloudBackupServerConfig cbsConfig;
	
	LoadingCache<UUID, Integer> packetIDCounts;	
	LoadingCache<UUID, Boolean> trackerPacketsComplete;
	int maxPacketCacheMinutes = 60;
	
	protected BackupStorageHandlerBase() {
		
		packetIDCounts = CacheBuilder.newBuilder()
			    .concurrencyLevel(1)
			    .expireAfterAccess(maxPacketCacheMinutes, TimeUnit.MINUTES)
			    .build(
			        new CacheLoader<UUID, Integer>() {
			            @Override
			        	public Integer load(UUID key) {
			            	return 0;
			          }
			        });
		
		//TODO Use different config for tracker complete timeout
		trackerPacketsComplete = CacheBuilder.newBuilder()
			    .concurrencyLevel(1)
			    .expireAfterAccess(maxPacketCacheMinutes, TimeUnit.MINUTES)
			    .build(
			        new CacheLoader<UUID, Boolean>() {
			            @Override
			        	public Boolean load(UUID key) {
			            	return false;
			          }
			        });
	}
	
	@Override 
	public void processBackupPacket(BackupFileDataPacket packet) throws Exception {
		
		if( null == packet ) {
			throw new Exception("packet can't be null");
		}
		
		logger.trace(String.format("In BackupStorageLocalHandler.processBackupPacket(BackupFileDataPacket packet): packetID = %s", packet.getDataPacketID()));
		
		UUID packetID = packet.getDataPacketID();
		UUID trackerID = packet.getFileTrackerID();
		
		BackupFileTracker tracker = dataSvc.getTrackerByBackupFileTrackerID(trackerID, packet.getClientID());
		
		UUID clientID = tracker.getClientID();
		
		boolean retryTracker = false;
		
		try {
			
			int dupPacketCount = checkPacketIDCount(packetID);
			
			if( 0 < dupPacketCount ) {				
				String dupPacketError = String.format("Skipping duplicate packet for tracker: %s, packet ID %s, packet number: %d, packet count = %d", 
						tracker.getBackupFileTrackerID().toString(), packetID.toString(), packet.getPacketNumber(), dupPacketCount);
				
				logger.info(dupPacketError);
				
//				tracker.setLastError(dupPacketError);
//				
//				dataSvc.updateBackupFileTracker(tracker);
				
				return;
			}
			
			if( FileAction.Delete == packet.getFileAction() ) {
				
				logger.info(String.format("Tracker: %s for client: %s, marking as deleted", 
											trackerID.toString(), clientID.toString()));
				
				if(false == tracker.isDirectory()) {
					this.deleteFinalTrackerFile(tracker);					
				}	
				
				tracker.setTrackerStatus(BackupFileTrackerStatus.Deleted);
				
			} else if( tracker.isDirectory() ) { //Only files are physically stored
				
				logger.info(String.format("Tracker: %s represents directory: %s for client: %s, marking as stored", 
											trackerID.toString(), tracker.getFileFullPath(), clientID.toString()));
				
				tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);
								
			} else {

				
				
				logger.info(String.format(" Processing update for tracker: %s representing file: %s for client: %s, packet %d of %d", 
											trackerID.toString(), tracker.getFileFullPath(),					
											clientID.toString(), packet.getPacketNumber(), packet.getPacketsTotal()));

				//First packet received
				if( BackupFileTrackerStatus.Pending == tracker.getTrackerStatus() || 
						BackupFileTrackerStatus.Retry == tracker.getTrackerStatus() ) {
					
					tracker = processFirstPacket(tracker.getBackupFileTrackerID(), tracker.getClientID());
					
				} else if(BackupFileTrackerStatus.Processing != tracker.getTrackerStatus()) {
					
					//Don't write status exception if already in error because we don't want to overwrite the 
					//original message
					if( BackupFileTrackerStatus.Error == tracker.getTrackerStatus()) {
						return;
					} else {
						throw new Exception(String.format("Invalid status for packet processing for tracker %s: status: %s",
							tracker.getBackupFileTrackerID().toString(), tracker.getTrackerStatus().toString()));
					}
				}
				
				preProcessPacketFile(tracker, packet);
				
				logger.info(String.format("Writing packet data for packet %s, packet number: %d of %d", 
								packetID.toString(), packet.getPacketNumber(), packet.getPacketsTotal()));
												
				try (ByteArrayInputStream bais = 
						new ByteArrayInputStream(Base64.getDecoder().decode(packet.getFileData()))) {
									
					try (OutputStream outputStream = this.getPacketFileOutputStream(tracker, packet.getPacketNumber())) {										
						FileZipUtils.WriteZipInputToOutput(bais, outputStream);														
					}
				}
				
				if(packet.getPacketNumber() == packet.getPacketsTotal()) {
					this.trackerPacketsComplete.put(tracker.getBackupFileTrackerID(), Boolean.TRUE);
				}

				//The packets may come out of order to keep checking after flag is tripped.
				//This is isolated because checking for all of the temp files over and over is
				//very expensive.
				if( Boolean.TRUE != this.trackerPacketsComplete.get(tracker.getBackupFileTrackerID()) ) {
					logger.info(String.format("Completed write, leaving temp file for tracker %s, last packet number: %d of %d", 
							trackerID.toString(), packet.getPacketNumber(), packet.getPacketsTotal()));
				} else {
					//Always retry If an exception happens in final processing
					//Note: Rework this if the call is no longer the last part of processing
					retryTracker = true;
				
					tracker = completeFullFileProcessing(trackerID, clientID, packet.getPacketNumber(), packet.getPacketsTotal(), packet.getFileChecksumDigest());
				} 
			}			
		} catch (Exception ex) {		
						
			logger.error("Unable to process data packets due to exception", ex);
			
			if( null != tracker ) {
				
				tracker.setLastError(ex.getMessage());
				
				if( retryTracker ) {
					tracker.setTrackerStatus(BackupFileTrackerStatus.Retry);
				} else {
					tracker.setTrackerStatus(BackupFileTrackerStatus.Error);
				}
								
			}			
			
		} finally {
			//Remove temp files if in any other state other than continuing to process
			if( BackupFileTrackerStatus.Processing != tracker.getTrackerStatus() ) {				
				
				if( BackupFileTrackerStatus.Stored != tracker.getTrackerStatus() ) {
					logger.info(String.format("Deleting temporary storage files tracker: %s", tracker.getBackupFileTrackerID().toString()));
				
					//Note: these methods should have no effect if the files don't exist
					this.deleteFinalTrackerFile(tracker);
					
					this.deletePacketFile(tracker, packet.getPacketNumber());
				}
			}
		}
				
		logger.info(String.format("Setting status and updating state for tracker: %s, status: %s", trackerID.toString(), tracker.getTrackerStatus().toString()));
		
		tracker.setLastStatusChange(new Date());
			
		this.dataSvc.updateBackupFileTracker(tracker);
		
		logger.trace(String.format("Completed BackupStorageLocalHandler.processBackupPacket(BackupFileDataPacket packet): packetID = %s", packet.getDataPacketID()));
	}

	synchronized private BackupFileTracker processFirstPacket(UUID trackerID, UUID clientID) throws Exception {
		
		//Get latest tracker in case another thread processed the first packet		
		BackupFileTracker tracker = this.dataSvc.getTrackerByBackupFileTrackerID(trackerID, clientID);
		
		if( BackupFileTrackerStatus.Pending != tracker.getTrackerStatus() && 
				BackupFileTrackerStatus.Retry != tracker.getTrackerStatus()) {			
			logger.info(String.format("Tracker no longer in pending or retry status for ID: %s, status: %s", trackerID.toString(), tracker.getTrackerStatus().toString()));
			return tracker;
		}
		
		tracker.setTrackerStatus(BackupFileTrackerStatus.Processing);
		tracker.setLastError("");
		
		if( true == this.checkTrackerFileExists(tracker) ) {
			this.deleteFinalTrackerFile(tracker);
		}
		
		if( true == this.checkTempFileExists(tracker) ) {
			this.deleteTempFile(tracker);
		}
		
		int packetNumberCount = 1;

		while( true == this.checkPacketFileExists(tracker, packetNumberCount) ) {
			this.deletePacketFile(tracker, packetNumberCount);
			
			packetNumberCount += 1;
		}

		handleFirstPacket(tracker);
		
		return tracker;
	}

	synchronized private int checkPacketIDCount(UUID packetID) throws ExecutionException, Exception {
		int dupPacketCount = packetIDCounts.get(packetID).intValue();
		
		if( 0 == dupPacketCount ) {
			packetIDCounts.put(packetID, 1);				
		} else {			
			packetIDCounts.put(packetID, dupPacketCount + 1);		
		}
		
		return dupPacketCount;
	}

	synchronized private BackupFileTracker completeFullFileProcessing(UUID trackerID, 
														   UUID clientID, 
														   int packetNumber,
														   int packetsTotal,			
														   String fileChecksumDigest) throws Exception {
			
		//Get latest tracker in case another thread completed processing
		BackupFileTracker tracker = this.dataSvc.getTrackerByBackupFileTrackerID(trackerID, clientID);
		
		if( BackupFileTrackerStatus.Processing != tracker.getTrackerStatus() ) {
			logger.info(String.format("Tracker no longer in processing status for ID: %s, status: %s", trackerID.toString(), tracker.getTrackerStatus().toString()));
			
			return tracker;
		}
		
		if( tracker.getFileAttributes().getFileSize() == 0 ) {
			logger.info(String.format("Creating zero length storage file for tracker: %s", tracker.getBackupFileTrackerID().toString()));
													
			try(InputStream inputStream = this.getPacketFileInputStream(tracker, 1)) {
				try(OutputStream outputStream = this.getFinalFileOutputStream(tracker)) {
					FileZipUtils.CreateZipOutputToStream(inputStream, 
														 outputStream,
														 tracker.getBackupFileTrackerID().toString());
				}
			}
			
			this.deletePacketFile(tracker, 1);
					
			tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);
		} else {					
		
			boolean isPacketsComplete = true;
			
			for(int packetNum = 1; packetNum <= packetsTotal && isPacketsComplete; packetNum++) {
				isPacketsComplete =(this.checkPacketFileExists(tracker, packetNum));
			}
			
			if( false == isPacketsComplete ) {	
				logger.info(String.format("Last packet received but not all packets filled for tracker: %s, last packet number: %d", 
								trackerID.toString(), packetNumber));
			} else {
							
				logger.info(String.format("Completed write for last packet of tracker: %s, comparing checksum digest", trackerID.toString())); 
				
				//Create digest for MD5 hash					
				MessageDigest messageDigest = (MessageDigest)MessageDigest.getInstance("MD5");
								
				try {
					try(OutputStream outputStream = getTempFileOutputStream(tracker)) {
						for(int packetNum = 1; packetNum <= packetsTotal; packetNum++) {
																					
							logger.info(String.format("Writing final packet data for tracker: %s, packet %d of %d",
									tracker.getBackupFileTrackerID().toString(), packetNum, packetsTotal));
															
							try(InputStream inputStream =  getPacketFileInputStream(tracker, packetNum)) {
								byte[] readBytes = new byte[4096];
							   
							    int numRead;
		
							    do {
							       numRead = inputStream.read(readBytes);
							       if (numRead > 0) {
							    	   outputStream.write(readBytes, 0, numRead);
							    	   messageDigest.update(readBytes, 0, numRead);
							       }
							    } while (numRead != -1);	      
							} finally {
								deletePacketFile(tracker, packetNum);							
							}								
						}
					}
					
					String md5Encoded = Base64.getEncoder().encodeToString(messageDigest.digest());
					
					if( true == md5Encoded.equals(fileChecksumDigest) ) {
						logger.info(String.format("Checksum match for tracker: %s, finalizing tracker record", trackerID.toString()));
										
						try(InputStream inputStream = this.getTempFileInputStream(tracker)) {
							try(OutputStream outputStream = this.getFinalFileOutputStream(tracker)) {
								FileZipUtils.CreateZipOutputToStream(inputStream, outputStream, tracker.getFileFullPath());
							}								
						}
						
						tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);								
					} else {
						logger.info(String.format("Checksum mis-match for tracker: %s, packet value: %s, calc value: %s, marking tracker for retry", 
								trackerID.toString(), packetsTotal, md5Encoded));
						
						this.deleteFinalTrackerFile(tracker);
		
						throw new Exception("Unable to create zip file due to checksum error");			
					} 
				} finally {
					this.trackerPacketsComplete.invalidate(tracker.getBackupFileTrackerID());
					
					this.deleteTempFile(tracker);
				}
			}
		}
		
		return tracker;
	} 	 

	@Override
	public void recreateTrackerFiles(BackupRestoreRequest restoreRequest) {
		
		//TODO Handle empty directories, currently not appearing in zip
		
		logger.trace("In BackupStorageLocalHandler.recreateTrackerFiles(BackupRestoreRequest restoreRequest)");
		
		if( null == restoreRequest ) {
			throw new NullPointerException("Restore request parameter can't be null");
		}
		
		logger.info(String.format("Restoring files to fore request: %s", restoreRequest.toString()));
				
		try {
			
			List<UUID> trackerIDList = restoreRequest.getRequestedFileTrackerIDs();			
						
			if( null == trackerIDList || 0 == trackerIDList.size() ) {
				throw new NullPointerException("Tracker list can't be null or empty");
			} 
			
			restoreRequest.setCurrentStatus(RestoreStatus.Initializing);
						
			restoreSvc.updateRestoreRequest(restoreRequest);
						
			final List<BackupFileTracker> trackerList = new ArrayList<BackupFileTracker>();
			
			for(UUID trackerID : trackerIDList) {
				BackupFileTracker bft = this.dataSvc.getTrackerByBackupFileTrackerID(trackerID, restoreRequest.getClientID());
				
				if( null == bft ) {
					throw new Exception(String.format("No file tracker found for id: %d", trackerID));
				} else if (!bft.getClientID().equals(restoreRequest.getClientID())) {
					throw new Exception(String.format("Client ID mismatch for file tracker: %d, found %s, expected %s", 
										trackerID, bft.getClientID(), restoreRequest.getClientID()));
				}
			
				logger.info(String.format("Adding requested tracker id: %s to restore reuqest", bft.getBackupFileTrackerID()));
				
				trackerList.add(bft);
			}
			
			if( true == restoreRequest.isIncludeSubdirectories() ) {
				logger.info(String.format("Checking for subdirectories in restore request: %s", restoreRequest.getRequestID().toString()));
								
				List<BackupFileTracker> additionalTrackerList = new ArrayList<BackupFileTracker>(); 
				
				List<BackupFileTracker> clientTrackerList = dataSvc.getAllBackupTrackersForClient(restoreRequest.getClientID());
				
				//Scan and add any child directories and files
				for(BackupFileTracker restoreTracker : trackerList) {
					if( true == restoreTracker.isDirectory() ) {
						scanForSubdirectories(restoreTracker, clientTrackerList, additionalTrackerList);
					}
				}
				
				trackerList.addAll(additionalTrackerList);
			}
			
			logger.info(String.format("Starting processing of %d tracker instances for restore request with id: %s", 
							trackerList.size(), restoreRequest.getRequestID().toString()));
			
			restoreRequest.setCurrentStatus(RestoreStatus.Processsing);
			restoreRequest.setProcessingStartDateTime(new Date());
			
			restoreSvc.updateRestoreRequest(restoreRequest);
			
			Enumeration<ZipEntryHelper> zipEntryHelperEnum = new Enumeration<ZipEntryHelper>() {							
				private int entryIndex = 0;
				private BackupFileTracker nextTracker = null;
				private boolean isInitialized = false;
				
				private void queueNextHelper() {
					nextTracker =  null;
				
					while(nextTracker == null && entryIndex < trackerList.size()) {
						//TODO: Handle empty directories					
						if( false == trackerList.get(entryIndex).isDirectory() ) {
							nextTracker = trackerList.get(entryIndex);
						}
						
						entryIndex += 1;
					}
					
					isInitialized = true;
				}
				
				
				@Override
				public boolean hasMoreElements() {
					if( false == isInitialized) {
						queueNextHelper();
					}
					
					return (null != nextTracker);
				}

				@Override
				public ZipEntryHelper nextElement() {		
					ZipEntryHelper retVal = null;
					try {
						retVal = createZipEntryHelperForTracker(restoreRequest, nextTracker);
					} catch (Exception ex) {
						logger.error(String.format("Error creating zip entry helper for tracker with ID: %s", 
										nextTracker.getBackupFileTrackerID().toString()), ex);
					}		
					
					queueNextHelper();
					
					return retVal;
				}				
			};		
			
			try(OutputStream outputStream = getFinalRestoreFileOutputStream(restoreRequest))	{
				FileZipUtils.CreateCompositeZipArchive(zipEntryHelperEnum, outputStream);
			}
						
			long totalFileSize = getFinalRestoreFileTotalSize(restoreRequest);
			
			logger.info(String.format("Final restore size for restore request: %s = %d", 
					restoreRequest.getRequestID().toString(), totalFileSize));
			
			if( totalFileSize > this.cbsConfig.getMaxRestoreSize() ) {
				throw new Exception("Maximum restore size reached, decrease requested restores");
			}
											
			restoreRequest.setCurrentStatus(RestoreStatus.Success);			
			restoreRequest.setCompletedDateTime(new Date());
			restoreRequest.setRestoreResultURL(this.getUrlForFinalRestoreFile(restoreRequest));
			
			restoreSvc.updateRestoreRequest(restoreRequest);
			
			logger.info(String.format("Restore complete for restore request: %s", restoreRequest.getRequestID().toString()));
			
		} catch (Exception ex) {			
			logger.error("Unable to recreate tracker files due to exception:", ex);
			
			try {
				
				restoreRequest.setCurrentStatus(RestoreStatus.Error);
				restoreRequest.setErrorMessage(ex.getLocalizedMessage());
				restoreRequest.setCompletedDateTime(new Date());
				
				restoreSvc.updateRestoreRequest(restoreRequest);
				
				this.deleteFinalRestoreFile(restoreRequest);

			} catch (Exception subEx) {
				logger.error("Unable to update restore request to error status due to execption:", ex);
			}
		}
		
		logger.trace("Completed BackupStorageLocalHandler.recreateTrackerFiles(BackupRestoreRequest restoreRequest)");
	}
	
	private void scanForSubdirectories(BackupFileTracker restoreTracker,
										List<BackupFileTracker> clientTrackerList,
										List<BackupFileTracker> additionalTrackerList ) {
		
		logger.info(String.format("Scanning %s for sub directories", restoreTracker.getFileFullPath()));
		
		for(BackupFileTracker clientTracker : clientTrackerList) {
			if( clientTracker.getFileFullPath().toLowerCase().startsWith(restoreTracker.getFileFullPath().toLowerCase()) &&
				false == clientTracker.getBackupFileTrackerID().equals(restoreTracker.getBackupFileTrackerID()) &&
				false == additionalTrackerList.contains(clientTracker) &&
				BackupFileTrackerStatus.Stored == clientTracker.getTrackerStatus()) {
				
				logger.info(String.format("Found tracker matching directory: %s - %s",
							restoreTracker.getFileFullPath(), clientTracker.getFileFullPath()));
				
				additionalTrackerList.add(clientTracker);
				
				if( true == clientTracker.isDirectory() ) {
					scanForSubdirectories(clientTracker, clientTrackerList, additionalTrackerList);				
				}
			}
		}		
	}

	private ZipEntryHelper createZipEntryHelperForTracker(BackupRestoreRequest restoreRequest, BackupFileTracker tracker) throws Exception {
		
		if(false == tracker.isDirectory() && false == this.checkTrackerFileExists(tracker)) {
			throw new Exception(String.format("Couldn't find stored file for tracker ID: %s",
									tracker.getBackupFileTrackerID()));
		}	
	
		logger.info(String.format("Restoring tracker %s, file name: %s, from request: %s", 
									tracker.getBackupFileTrackerID().toString(), tracker.getFileFullPath(), restoreRequest.getRequestID().toString()));
		
		//Check for external change of request status
		BackupRestoreRequest latestRestoreRequest = restoreSvc.getRestoreRequestByRequestID(restoreRequest.getRequestID());
		String entryName = null;
		InputStream inputStream = null;
		
		if( null != latestRestoreRequest && RestoreStatus.Processsing == latestRestoreRequest.getCurrentStatus() ) {

			//TODO Make configurable
			int driveLetterIdx = tracker.getFileFullPath().indexOf(":");
			
			if( 0 > driveLetterIdx ) {
				entryName = tracker.getFileFullPath();
			} else {
				entryName = tracker.getFileFullPath().substring(driveLetterIdx + 1);
			}
			
			//inputStream = trackerBlob.openInputStream();
			inputStream = this.getFinalFileInputStream(tracker);
			
		} else { //State has changed since request started
											
			throw new Exception(String.format("Status has changed for request ID: %s since start of restore process, aborting", restoreRequest.getRequestID()));
		}
		
		return new ZipEntryHelper(entryName, inputStream, true);
	}
	
	abstract protected boolean checkTrackerFileExists(BackupFileTracker tracker) throws Exception;
	
	abstract protected void deleteFinalTrackerFile(BackupFileTracker tracker) throws Exception;

	abstract protected void deleteFinalRestoreFile(BackupRestoreRequest restoreRequest) throws Exception;
	
	abstract protected void handleFirstPacket(BackupFileTracker tracker) throws Exception;
	
	abstract protected void preProcessPacketFile(BackupFileTracker tracker, BackupFileDataPacket packet) throws Exception;
			
	abstract protected InputStream getPacketFileInputStream(BackupFileTracker tracker, int packetNum) throws Exception;
	
	abstract protected OutputStream getPacketFileOutputStream(BackupFileTracker tracker, int packetNum) throws Exception;

	abstract protected boolean checkPacketFileExists(BackupFileTracker tracker, int i) throws Exception;
	
	abstract protected OutputStream getFinalFileOutputStream(BackupFileTracker tracker) throws Exception;
	
	abstract protected InputStream getFinalFileInputStream(BackupFileTracker tracker) throws Exception;

	abstract protected boolean checkTempFileExists(BackupFileTracker tracker) throws Exception;
	
	abstract protected InputStream getTempFileInputStream(BackupFileTracker tracker) throws Exception;

	abstract protected OutputStream getTempFileOutputStream(BackupFileTracker tracker) throws Exception;

	abstract protected void deleteTempFile(BackupFileTracker tracker) throws Exception;
	
	abstract protected void deletePacketFile(BackupFileTracker tracker, int packetNum) throws Exception;	
				
	abstract protected OutputStream getFinalRestoreFileOutputStream(BackupRestoreRequest restoreRequest) throws Exception;

	protected abstract String getUrlForFinalRestoreFile(BackupRestoreRequest restoreRequest) throws Exception;

	abstract protected long getFinalRestoreFileTotalSize(BackupRestoreRequest restoreRequest) throws Exception;
		
}
