package com.nearskysolutions.cloudbackup.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nearskysolutions.cloudbackup.client.CloudBackupClient;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket.FileAction;
import com.nearskysolutions.cloudbackup.services.BackupFileDataPacketService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CloudBackupClientTestConfig.class)
@Transactional
public class BackupFileDataPacketServiceTest {

Logger logger = LoggerFactory.getLogger(CloudBackupClient.class);
	
	@Autowired	
	private BackupFileDataPacketService packetSvc;
		
	@Test
	public void TestIDQuery() {
		
				
		BackupFileDataPacket dataPacket = new BackupFileDataPacket(100, 10, 1, 10, new Date(), "abcd", FileAction.Create);
		
		try {
			dataPacket = packetSvc.addBackupFileDataPacket(dataPacket);
		
			Long updateID = dataPacket.getFileUpdateID();
			
			BackupFileDataPacket packet = packetSvc.getPacketByFileUpdateID(updateID);
			
			assertNotNull(packet);
			assertEquals(updateID, packet.getFileUpdateID());
			
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
	
		
	}
	
	@Test
	public void TestDateQuery() {
		
		Calendar createDateTime1 = Calendar.getInstance();
		Calendar createDateTime2 = Calendar.getInstance();
		
		Date compareDateTime = createDateTime1.getTime();
		
		createDateTime2.setTimeInMillis(createDateTime1.getTimeInMillis() + 2000);
		
		BackupFileDataPacket dataPacket1 = new BackupFileDataPacket(100, 10, 1, 10, createDateTime1.getTime(), 
																	"abcd", FileAction.Create);
		
		BackupFileDataPacket dataPacket2 = new BackupFileDataPacket(200, 20, 2, 10, createDateTime2.getTime(), 
																	"1234", FileAction.Create);
				
		try {
			packetSvc.addBackupFileDataPacket(dataPacket1);
			packetSvc.addBackupFileDataPacket(dataPacket2);
			
			List<BackupFileDataPacket> packetList = packetSvc.getPacketsCreatedAfter(compareDateTime);
			
			assertNotNull(packetList);
			assertTrue(packetList.size() > 0);
			
	        for (BackupFileDataPacket packet : packetList) {
				assertTrue(packet.getDateTimeCaptured().compareTo(compareDateTime) > 0);
			}
		} catch (Exception e) {
		
			logger.error("Error: ", e);
			e.printStackTrace();
		}
	
		
	}
	
}
