package com.nearskysolutions.cloudbackup.test.beans;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.nearskysolutions.cloudbackup.common.BackupFileDataPacket;
import com.nearskysolutions.cloudbackup.common.BackupRestoreRequest;
import com.nearskysolutions.cloudbackup.common.FilePacketHandlerQueue;
import com.nearskysolutions.cloudbackup.common.RestoreRequestHandlerQueue;

@Component(value="TestRestoreRequestHandler")
public class RestoreRequestHandlerQueueTest implements RestoreRequestHandlerQueue {

	private List<BackupRestoreRequest> requestList;
	
	public RestoreRequestHandlerQueueTest() {
		requestList = new ArrayList<BackupRestoreRequest>();	
	}

	@Override
	public boolean queueHasRequests() {
		return (this.requestList.size() > 0);
	}

	@Override
	public void queueRequest(BackupRestoreRequest restoreRequest) throws Exception {
		this.requestList.add(restoreRequest);		
	}

	@Override
	public BackupRestoreRequest retreiveNextRestoreRequest() throws Exception {		
		BackupRestoreRequest retVal = null;
		
		if(this.requestList.size() > 0) {
			retVal = this.requestList.get(0);
			this.requestList.remove(0);
		}
			
		return retVal;
	}

}
