package com.nearskysolutions.cloudbackup.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.nearskysolutions.cloudbackup.common.BackupFileClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.services.BackupFileClientService;

@Component(value="AzureBackupStorage")
public class BackupStorageAzureHandler extends BackupStorageHandlerBase {
	
	Logger logger = LoggerFactory.getLogger(BackupStorageAzureHandler.class);
		
	@Autowired 
	private BackupFileClientService clientSvc;
	
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
                
        return blobClient.getContainerReference(this.restoreContainer.toLowerCase());
    }
	
	
	
	@Override
	protected void deleteFinalTrackerFile(BackupFileTracker tracker) throws Exception {
		CloudBlob finalCloudBlob = getFinalCloudBlobZip(tracker);				
				
		finalCloudBlob.deleteIfExists();		
	}

	@Override
	protected void handleFirstPacket(BackupFileTracker tracker) throws Exception {
		
	}

	@Override
	protected void preProcessPacketFile(BackupFileTracker tracker, BackupFileDataPacket packet) throws Exception {
		CloudBlob packetBlob = getPacketCloudBlob(tracker, packet.getPacketNumber());
		
		packetBlob.deleteIfExists();				
	}
	
	@Override
	protected InputStream getPacketFileInputStream(BackupFileTracker tracker, int packetNum) throws Exception {
		CloudBlob cloudBlob = getPacketCloudBlob(tracker, packetNum);
		
		return cloudBlob.openInputStream();		
	}
	
	@Override
	protected OutputStream getPacketFileOutputStream(BackupFileTracker tracker, int packetNum) throws Exception {		
		CloudBlockBlob cloudBlob = getPacketCloudBlob(tracker, packetNum);
		
		return cloudBlob.openOutputStream();			
	}

	@Override
	protected OutputStream getFinalFileOutputStream(BackupFileTracker tracker) throws Exception {
		CloudBlockBlob cloudBlob = getFinalCloudBlobZip(tracker);
		
		return cloudBlob.openOutputStream();		
	}

	@Override
	protected InputStream getFinalFileInputStream(BackupFileTracker tracker) throws Exception {
		CloudBlob cloudBlob = getFinalCloudBlobZip(tracker);
		
		return cloudBlob.openInputStream();
	}

	@Override
	protected boolean checkTempFileExists(BackupFileTracker tracker) throws Exception {
		CloudBlob tempBlob = getTempCloudBlob(tracker);
		
		return tempBlob.exists();
	}
	
	@Override
	protected InputStream getTempFileInputStream(BackupFileTracker tracker) throws Exception {
		CloudBlob tempBlob = getTempCloudBlob(tracker);
		
		return tempBlob.openInputStream();		
	}

	@Override
	protected OutputStream getTempFileOutputStream(BackupFileTracker tracker) throws Exception {
		CloudBlockBlob tempBlob = getTempCloudBlob(tracker);
		
		return tempBlob.openOutputStream();
	}
	
	@Override
	protected void deleteTempFile(BackupFileTracker tracker) throws Exception {
		CloudBlob tempBlob = getTempCloudBlob(tracker);
		
		tempBlob.deleteIfExists();
	}
	
	@Override
	protected void deletePacketFile(BackupFileTracker tracker, int packetNum) throws Exception {
		CloudBlob packetBlob = getPacketCloudBlob(tracker, packetNum);
		
		packetBlob.deleteIfExists();	
	}
	
	@Override 
	protected boolean checkPacketFileExists(BackupFileTracker tracker, int packetNum) throws Exception {
		CloudBlob packetBlob = getPacketCloudBlob(tracker, packetNum);
		
		return packetBlob.exists();	
	}
	
	@Override
	protected boolean checkTrackerFileExists(BackupFileTracker tracker) throws Exception {
		CloudBlob finalCloudBlob = getFinalCloudBlobZip(tracker);
		
		return finalCloudBlob.exists();
	}

	@Override
	protected void deleteFinalRestoreFile(BackupRestoreRequest restoreRequest) throws Exception {
		CloudBlob restoreBlob = getRestoreBlob(restoreRequest);
		
		restoreBlob.deleteIfExists();		
	}
	
	@Override
	protected OutputStream getFinalRestoreFileOutputStream(BackupRestoreRequest restoreRequest) throws Exception {				
		CloudBlockBlob restoreBlob = getRestoreBlob(restoreRequest);
		
		return restoreBlob.openOutputStream();
	}

	@Override
	protected String getUrlForFinalRestoreFile(BackupRestoreRequest restoreRequest) throws Exception {
		CloudBlob restoreBlob = getRestoreBlob(restoreRequest);
		
		return restoreBlob.getUri().toString();
	}

	@Override
	protected long getFinalRestoreFileTotalSize(BackupRestoreRequest restoreRequest) throws Exception {
		CloudBlob restoreBlob = getRestoreBlob(restoreRequest);
		
		return (restoreBlob.exists() ? restoreBlob.getProperties().getLength() : 0);		
	}

	
	private CloudBlockBlob getTrackerBlob(BackupFileTracker tracker, String suffix) throws Exception {
		return getCloudBlobWithName(getContainerForClientBackups(tracker.getClientID()), tracker.getClientID(), tracker.getBackupFileTrackerID().toString(), suffix);
	}
	
	private CloudBlockBlob getRestoreBlob(BackupRestoreRequest restoreRequest) throws Exception {
		return getCloudBlobWithName(getContainerForClientRestoreArchive(restoreRequest.getClientID()), restoreRequest.getClientID(), restoreRequest.getRequestID().toString(), "zip");
	}
	
	private CloudBlockBlob getCloudBlobWithName(CloudBlobContainer blobContainer, UUID clientID, String name, String suffix) throws Exception {					
				
		String blobName = String.format("%s.%s", name, suffix);
		
		return blobContainer.getBlockBlobReference(blobName);
	}
	
	private CloudBlockBlob getFinalCloudBlobZip(BackupFileTracker tracker) throws Exception {				
		return getTrackerBlob(tracker, "zip");
	}
	
	private CloudBlockBlob getTempCloudBlob(BackupFileTracker tracker) throws Exception {				
		return getTrackerBlob(tracker, "tmp");
	}
	
	private CloudBlockBlob getPacketCloudBlob(BackupFileTracker tracker, int packetNumber) throws Exception {				
		return getTrackerBlob(tracker, String.format("%d", packetNumber));
	}
}
