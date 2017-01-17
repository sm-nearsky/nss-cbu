package com.nearskysolutions.cloudbackup.test.beans;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.FilePacketHandlerQueue;

@Component(value="TestPacketHandler")
public class PacketHandlerQueueTest implements FilePacketHandlerQueue {

	private List<BackupFileDataPacket> packetList;
	
	public PacketHandlerQueueTest() {
		packetList = new ArrayList<BackupFileDataPacket>();	
	}
	
	@Override
	public void queuePacket(BackupFileDataPacket packet)  throws Exception {
		this.packetList.add(packet);
	}

	@Override
	public void removePacketsForBatch(Long batchID) throws Exception {

		for(int i = packetList.size(); --i >= 0;) {
			if( batchID.longValue() == packetList.get(i).getFileBatchID().longValue() ) {
				packetList.remove(i);
			}
		}		
	}	
	
	public List<BackupFileDataPacket> getPacketQueue() {
		return this.packetList;
	}

	@Override
	public boolean queueHasPackets() {		
		return (this.packetList.size() > 0);
	}

	@Override
	public List<BackupFileDataPacket> retreivePacketsForBatch(Long batchID) throws Exception {

		List<BackupFileDataPacket> filteredList = new ArrayList<BackupFileDataPacket>();
		
		for(BackupFileDataPacket packet : this.packetList) {
			if( packet.getFileBatchID().longValue() == batchID.longValue() ) {
				filteredList.add(packet);
			}
		}
		
		return filteredList;
	}

}
