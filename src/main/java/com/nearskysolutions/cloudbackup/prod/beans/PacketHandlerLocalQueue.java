package com.nearskysolutions.cloudbackup.prod.beans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileTracker;
import com.nearskysolutions.cloudbackup.common.FilePacketHandlerQueue;
import com.nearskysolutions.cloudbackup.data.BackupFileTrackerRepository;
import com.nearskysolutions.cloudbackup.services.BackupFileDataService;

@Component(value="LocalPacketHandler")
public class PacketHandlerLocalQueue implements FilePacketHandlerQueue {

	Logger logger = LoggerFactory.getLogger(PacketHandlerLocalQueue.class);
	
	@Value( "${com.nearskysolutions.cloudbackup.queue.localQueueDir}" )
	private String localQueueDir;

	@Value( "${com.nearskysolutions.cloudbackup.general.filePacketSize}" )
	private int filePacketSize;
	
	@Autowired 
	private BackupFileDataService dataSvc;
	
	public PacketHandlerLocalQueue() {
			
	}
	
	@Override
	public boolean queueHasPackets() {
		
		logger.trace("Called PacketHandlerLocalQueue.queueHasPackets");
		
		File queueDir = new File(this.localQueueDir);
		boolean retVal = false;
		
		if(false == queueDir.exists()) {
			logger.info("Packet queue directory: %s does not exist, returning false from PacketHandlerLocalQueue.queueHasPackets");
		} else {
			//Note: Not checking for correct format, only files exist
			retVal = (queueDir.listFiles().length > 0);
		}
		
		logger.trace("Returning %s from PacketHandlerLocalQueue.queueHasPackets", retVal);
		
		return retVal;
	}
	
	@Override
	public void queuePacket(BackupFileDataPacket packet)  throws Exception {
	
		logger.trace(String.format("Called PacketHandlerLocalQueue.queuePacket passing packet: %s", packet));
		
		if( null == packet ) {
			throw new NullPointerException("BackupFileDataPacket reference can't be null");
		}
		
		logger.info(String.format("Creating local file queue entry for batch ID: %d, packet ID: %d and packet #: %d", 
									packet.getFileBatchID(), 
									packet.getDataPacketID(),
									packet.getPacketNumber()));
		
		FileInputStream fis = null;
		FileOutputStream fos = null;
		
		try {
			
			BackupFileTracker tracker = this.dataSvc.getTrackerByBackupFileTrackerID(packet.getFileTrackerID());
			
			//Make sure file exists, i.e. not in a deleted state and file isn't a directory
			//which doesn't need encoded data
			if( packet.getFileReference().exists() && false == tracker.isDirectory() ) {
				//Get packet data
				logger.info(String.format("Reading packet file: %s", packet.getFileReference().getAbsolutePath()));
						
				fis = new FileInputStream(packet.getFileReference().getAbsolutePath());
						
				ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
				byte[] bytes = new byte[this.filePacketSize];
				int count;
				
				while((count = fis.read(bytes)) > 0) {
					byteOutput.write(bytes,	0, count);
				};
				
				byte[] fileBytes = byteOutput.toByteArray();
				
				fis.close();
				fis = null;
				
				logger.trace(String.format("%d bytes read for file %s", fileBytes.length, packet.getFileReference().getAbsolutePath()));
	
				//Convert bytes to base64
				logger.info(String.format("Converting file bytes to base 64 for file: %s", packet.getFileReference().getAbsolutePath()));
				
				String encodedBytes = Base64.getEncoder().encodeToString(fileBytes);
				
				//Add bytes to packet for transport
				packet.setFileData(encodedBytes);
			}
			
			File parentDir = new File(this.localQueueDir);
			
			if( false == parentDir.exists() ) {
				parentDir.mkdir();
			}
			
			//Save packet as XML and output
			String outputFileName = String.format("%s%sbatch%d_packet%d_number%d.xml", this.localQueueDir, 
																						  File.separator, 
																						  packet.getFileBatchID(), 
																						  packet.getDataPacketID(), 
																						  packet.getPacketNumber());
			
			fos = new FileOutputStream(outputFileName);
			
			logger.info(String.format("Writing coded object data to queue file for packet ID: %d:", 
										packet.getDataPacketID()));

			JAXBContext jc = JAXBContext.newInstance( BackupFileDataPacket.class );
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(packet, fos);
			
			fos.close();
			fos = null;
			
		} finally {
			if( null != fis ) {
				fis.close();
			}
			
			if( null != fos ) {
				fos.close();
			}
		}
		
		logger.trace("Finished call to PacketHandlerLocalQueue.queuePacket for packet: %s", packet);
	}

	@Override
	public void removePacketsForBatch(Long batchID) throws Exception {

		logger.trace(String.format("Called PacketHandlerLocalQueue.removePacketsForBatch passing batch ID: %d", batchID));
		
		logger.info(String.format("Scanning queue directory: %s for packets matching batch ID: %d for removal", this.localQueueDir, batchID));
		
		File queueDir = new File(this.localQueueDir);
		
		for(File file : queueDir.listFiles()) {
			if(file.getName().toLowerCase().startsWith(String.format("batch%d",batchID))) {
				
				logger.info(String.format("Found packet file to remove for batch ID %d: , %s:", batchID, file.getAbsoluteFile()));
				
				file.delete();
			}
		}
		
		logger.trace(String.format("Finished call to PacketHandlerLocalQueue.removePacketsForBatch with batch ID: %d", batchID));
	}	

	@Override
	public List<BackupFileDataPacket> retreivePacketsForBatch(Long batchID) throws Exception {
		
		logger.trace("Called PacketHandlerLocalQueue.retreivePacketsForBatch");
		
		File queueDir = new File(this.localQueueDir);
		List<BackupFileDataPacket> packetList = new ArrayList<BackupFileDataPacket>();
		BackupFileDataPacket packet;
		
		if(false == queueDir.exists()) {
			logger.info(String.format("Packet queue directory: %s does not exist, returning null from PacketHandlerLocalQueue.dequeueNextPacket"));
		} else {						
			List<File> filteredList = new ArrayList<File>();			
						
			for(File file : queueDir.listFiles()) {
				if(file.getName().toLowerCase().startsWith(String.format("batch%d",batchID))) {
					
					logger.info(String.format("Found packet file for batch ID %d: , %s:", batchID, file.getAbsoluteFile()));
					
					filteredList.add(file);
				}
			}
			
			//Note: Not checking for correct format, only files exist
			if( 0 == filteredList.size() ) {
				logger.info(String.format("No packets found for batch ID: %d", batchID));
			} else {			
				filteredList.sort(new Comparator<File>() {			    
					@Override
					public int compare(File f1, File f2) {			
						return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
					} });
				
				for(File file : filteredList) {
					packet = recreatePacketFromFile(file);
				
					packetList.add(packet);
					
					file.delete();
					
					logger.info(String.format("Returning packet: %d for batch id: %d", packet.getDataPacketID(), batchID));
				}						
			}
		}
		
		logger.trace(String.format("Returning %d packets for batchID %d", packetList.size(), batchID));
		
		return packetList;
	}	
	
	private BackupFileDataPacket recreatePacketFromFile(File packetFile) throws Exception {
		
		FileInputStream fis = null;
		BackupFileDataPacket packet = null;
		
		try {			
						
			fis = new FileInputStream(packetFile);
				
			JAXBContext jc = JAXBContext.newInstance( BackupFileDataPacket.class );
			Unmarshaller um = jc.createUnmarshaller();

			packet = (BackupFileDataPacket)um.unmarshal(fis);
			
		}finally {
			if( null != fis) {
				fis.close();				
			}		
		}		
		
		return packet;		
	}
}
