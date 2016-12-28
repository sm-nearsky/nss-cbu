package com.nearskysolutions.cloudbackup.services;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface FileHandlerService {
			
	void updateFileTrackerListing(UUID clientID, String rootDir) throws IOException;
	
	void storePacketsForFile(File file) throws Exception;
}
