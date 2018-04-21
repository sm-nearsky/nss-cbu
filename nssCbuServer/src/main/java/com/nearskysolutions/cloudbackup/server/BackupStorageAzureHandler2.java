package com.nearskysolutions.cloudbackup.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.BlobOutputStream;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker.BackupFileTrackerStatus;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest.RestoreStatus;
import com.nearskysolutions.cloudbackup.common.BackupStorageHandler;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;
import com.nearskysolutions.cloudbackup.services.BackupRestoreRequestService;
import com.nearskysolutions.cloudbackup.util.FileZipUtils;
import com.nearskysolutions.cloudbackup.util.ZipEntryHelper;

@Component(value="AzureBackupStorage")
public class BackupStorageAzureHandler implements BackupStorageHandler {
	
	Logger logger = LoggerFactory.getLogger(BackupStorageAzureHandler.class);
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	@Autowired 
	private BackupFileClientService clientSvc;
	
	@Autowired
	private BackupRestoreRequestService restoreSvc;
	
	@Autowired
	CloudBackupServerConfig cbsConfig;
	
	@Value( "${com.nearskysolutions.cloudbackup.azurestore.containerCSTemplate}" )
	private String containerCSTemplate;
	
	@Value( "${com.nearskysolutions.cloudbackup.azurestore.restoreContainer}" )
	private String restoreContainer;
	
	@Value( "${com.nearskysolutions.cloudbackup.azurestore.clientBackupContainerPrefix}" )
	private String clientBackupContainerPrefix;
	
	public BackupStorageAzureHandler() {
		
	}
	
	@Cacheable
	private CloudBlobContainer getContainerForClientBackups(UUID clientID) throws Exception {
		BackupFileClient client = this.clientSvc.getBackupClientByClientID(clientID);
		
		String connString = this.containerCSTemplate.replaceAll("#ACCOUNT_NAME#", client.getCurrentRepositoryLocation())
													.replaceAll("#ACCOUNT_KEY#", client.getCurrentRepositoryKey());
				
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient(); 
                
        return blobClient.getContainerReference(String.format("%s%s", 
        										this.clientBackupContainerPrefix, 
        										client.getClientID().toString().toLowerCase()));
    }
	 
	@Cacheable
	private CloudBlobContainer getContainerForClientRestoreArchive(UUID clientID) throws Exception {
		BackupFileClient client = this.clientSvc.getBackupClientByClientID(clientID);
		
		String connString = this.containerCSTemplate.replaceAll("#ACCOUNT_NAME#", client.getCurrentRepositoryLocation())
												    .replaceAll("#ACCOUNT_KEY#", client.getCurrentRepositoryKey());
		
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(connString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient(); 
                
        return blobClient.getContainerReference(this.restoreContainer);
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
		CloudBlobContainer blobContainer = getContainerForClientBackups(clientID);
						
		String tmpBlobName = String.format("%s.tmp", trackerID.toString());
		
		String finalBlobName = String.format("%s.zip", trackerID.toString());
		
		boolean retryTracker = false;
		
		CloudAppendBlob tmpBlob = blobContainer.getAppendBlobReference(tmpBlobName);
		CloudAppendBlob finalBlob = blobContainer.getAppendBlobReference(finalBlobName);
		
		try {				
			
			if( FileAction.Delete == packet.getFileAction() ) {
				
				logger.info(String.format("Tracker: %s for client: %s, marking as deleted", 
											trackerID.toString(), clientID.toString()));
				
				if(false == tracker.isDirectory() && 
						(tmpBlob.exists() || finalBlob.exists())) {	
					
					logger.info(String.format("Deleting stored files for tracker: %s for client: %s", 
												trackerID.toString(), clientID.toString()));
										
					//Delete files associated with tracker
					tmpBlob.deleteIfExists();					
					finalBlob.deleteIfExists();
				}	
				
				tracker.setTrackerStatus(BackupFileTrackerStatus.Deleted);
				
			} else if( tracker.isDirectory() ) { //Only files are physically stored
				
				logger.info(String.format("Tracker: %s represents directory: %s%s%s for client: %s, marking as stored", 
											trackerID.toString(), tracker.getSourceDirectory(), File.separator, tracker.getFileName(), clientID.toString()));
				
				tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);
								
			} else {

				logger.info(String.format(" Processing update for tracker: %s representing file: %s%s%s for client: %s, packet %d of %d", 
											trackerID.toString(), tracker.getSourceDirectory(), File.separator, tracker.getFileName(), 
											clientID.toString(), packet.getPacketNumber(), packet.getPacketsTotal()));
								
				if( 1 != packet.getPacketNumber() ) {
				
					if(BackupFileTrackerStatus.Processing != tracker.getTrackerStatus() &&
							BackupFileTrackerStatus.Retry != tracker.getTrackerStatus()) {						
						retryTracker = true;
						
						throw new Exception(String.format("Invalid state - packet number out of order and can't process for packetID: %s, tracker ID: %s, packet #: %d, state: %s", 
															packetID.toString(), trackerID.toString(), packet.getPacketNumber(), tracker.getTrackerStatus().toString()));						
					} else if(false == tmpBlob.exists() ) {						
						retryTracker = true;
					
						throw new Exception(String.format("Not first packet and no temp file for packetID: %s, tracker ID: %s", packetID.toString(), trackerID.toString()));
					} 
					
				} else {				
				
					tracker.setTrackerStatus(BackupFileTrackerStatus.Processing);
					tracker.setLastError("");
					
					logger.info(String.format("Starting new stored file for tracker ID: %s, client ID: %s", trackerID.toString(), clientID.toString()));
						
					//Replace final file or temp file if either exist and this is the first new packet
					finalBlob.deleteIfExists();
										
					tmpBlob.createOrReplace();
				}
				
				logger.info(String.format("Writing packet data for packet %s, packet number: %d of %d", 
								packetID.toString(), packet.getPacketNumber(), packet.getPacketsTotal()));
				
				try (ByteArrayInputStream bais = 
						new ByteArrayInputStream(Base64.getDecoder().decode(packet.getFileData()))) {									
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {										
						FileZipUtils.WriteZipInputToOutput(bais, baos);
						byte[] zipBytes = baos.toByteArray();
						tmpBlob.appendFromByteArray(zipBytes, 0, zipBytes.length);
					}
				}
				
				logger.info(String.format("Saving last byte count saved: %d for tracker: %s", 
											packet.getEndingFileByteCount(), tracker.getBackupFileTrackerID().toString()));
				tracker.setLastByteSent(packet.getEndingFileByteCount());
				this.dataSvc.updateBackupFileTracker(tracker);
				
				if( packet.getPacketsTotal() != packet.getPacketNumber() ) {	
					logger.info(String.format("Completed write, leaving temp file for packet %s, last packet number: %d of %d", 
							packetID.toString(), packet.getPacketNumber(), packet.getPacketsTotal()));
				} else {
								
					logger.info(String.format("Completed write for last packet of tracker: %s, comparing checksum digest", trackerID.toString())); 
					
					if( packet.getEndingFileByteCount() == 0 ) {
						logger.info(String.format("Creating zero length container file for tracker: %s", trackerID.toString()));
																		
						try(BlobInputStream bis = tmpBlob.openInputStream()) {
							try(BlobOutputStream bos = finalBlob.openWriteNew()) {
								FileZipUtils.CreateZipOutputToStream(bis, bos, tracker.getFileFullPath());
							}
						}						
						
						tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);
					} else {
						//Create digest for MD5 hash					
						MessageDigest messageDigest = (MessageDigest)MessageDigest.getInstance("MD5");
												
						try(BlobInputStream bis =  tmpBlob.openInputStream()) {
							byte[] mdBytes = new byte[4096];
						   
						    int numRead;
	
						    do {
						       numRead = bis.read(mdBytes);
						       if (numRead > 0) {
						    	   messageDigest.update(mdBytes, 0, numRead);
						       }
						    } while (numRead != -1);	      
						}
						
						String md5Encoded = Base64.getEncoder().encodeToString(messageDigest.digest());
						
						if( false == md5Encoded.equals(packet.getFileChecksumDigest()) ) {						
							logger.info(String.format("Checksum mis-match for tracker: %s, packet value: %s, calc value: %s, marking tracker for retry", 
														trackerID.toString(), packet.getFileChecksumDigest(), md5Encoded));
							
							retryTracker = true;
							
							throw new Exception("Unable to create zip file due to checksum error");
						} else {						
							logger.info(String.format("Checksum match for tracker: %s, creating final container file", trackerID.toString()));
							
							try(BlobInputStream bis = tmpBlob.openInputStream()) {
								try(BlobOutputStream bos = finalBlob.openWriteNew()) {
									FileZipUtils.CreateZipOutputToStream(bis, bos, tracker.getFileFullPath());
								}								
							}
							
							tracker.setTrackerStatus(BackupFileTrackerStatus.Stored);
						}	
					}
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
			
			//Remove temp file if in any other state other than continuing to process
			if( BackupFileTrackerStatus.Processing != tracker.getTrackerStatus() && true == tmpBlob.exists() ) {				
				logger.info(String.format("Deleting temporary storage file tracker: %s", trackerID.toString()));
				
				tmpBlob.delete();
			}
			
		}
		
		
		logger.info(String.format("Setting status and updating state for tracker: %s, status: %s", trackerID.toString(), tracker.getTrackerStatus().toString()));
		
		tracker.setLastStatusChange(new Date());
			
		this.dataSvc.updateBackupFileTracker(tracker);
		
		logger.trace(String.format("Completed BackupStorageLocalHandler.processBackupPacket(BackupFileDataPacket packet): packetID = %s", packet.getDataPacketID()));
	} 	 
	
	@Override
	public void recreateTrackerFiles(BackupRestoreRequest restoreRequest) {
		
		//TODO Handle empty directories, currently not appearing in zip
		
		logger.trace("In BackupStorageLocalHandler.recreateTrackerFiles(BackupRestoreRequest restoreRequest)");
		
		logger.info(String.format("Restoring files to archive container: %s", this.restoreContainer));
		
		CloudAppendBlob restoreBlob = null;
		
		try {			
			
			if( null == restoreRequest ) {
				throw new NullPointerException("Restore request parameter can't be null");
			}
			
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
									
			UUID requestID = restoreRequest.getRequestID();		
			UUID clientID = restoreRequest.getClientID();
			String requestBlobName = String.format("%s.zip", requestID.toString().toLowerCase());
			
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
			
			restoreBlob = getContainerForClientRestoreArchive(clientID).getAppendBlobReference(requestBlobName);
			
			if(false == restoreBlob.exists()) {
				logger.info(String.format("Creating restore blob for request: %s", requestID.toString()));
				
				restoreBlob.createOrReplace();
			}
			
			try(BlobOutputStream bos = restoreBlob.openWriteExisting())	{
				FileZipUtils.CreateCompositeZipArchive(zipEntryHelperEnum, bos);
			}
			
			long totalFileSize = restoreBlob.getProperties().getLength();
			
			logger.info(String.format("Final restore size for restore request: %s = %d", 
					restoreRequest.getRequestID().toString(), totalFileSize));
			
			if( totalFileSize > this.cbsConfig.getMaxRestoreSize() ) {
				throw new Exception("Maximum restore size reached, decrease requested restores");
			}
											
			restoreRequest.setCurrentStatus(RestoreStatus.Success);			
			restoreRequest.setCompletedDateTime(new Date());								
			restoreRequest.setRestoreResultURL(restoreBlob.getUri().toString());
			
			restoreSvc.updateRestoreRequest(restoreRequest);
			
			logger.info(String.format("Restore complete for restore request: %s", restoreRequest.getRequestID().toString()));
			
		} catch (Exception ex) {			
			logger.error("Unable to recreate tracker files due to exception:", ex);
			
			try {
				restoreRequest.setCurrentStatus(RestoreStatus.Error);
				restoreRequest.setErrorMessage(ex.getLocalizedMessage());
				restoreRequest.setCompletedDateTime(new Date());
				
				restoreSvc.updateRestoreRequest(restoreRequest);
				
				if( null != restoreBlob ) {
					restoreBlob.deleteIfExists();
				}
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
		
		CloudBlob trackerBlob = getContainerForClientBackups(restoreRequest.getClientID())
											.getAppendBlobReference(String.format("%s.zip", tracker.getBackupFileTrackerID().toString()));
	
		if(false == tracker.isDirectory() && false == trackerBlob.exists()) {
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
			
			inputStream = trackerBlob.openInputStream();			
			
		} else { //State has changed since request started
											
			throw new Exception(String.format("Status has changed for request ID: %s since start of restore process, aborting", restoreRequest.getRequestID()));
		}
		
		return new ZipEntryHelper(entryName, inputStream, true);
	}
}
