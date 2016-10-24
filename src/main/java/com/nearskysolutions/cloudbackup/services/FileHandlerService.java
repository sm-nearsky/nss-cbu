package com.nearskysolutions.cloudbackup.services;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface FileHandlerService {

	List<File> scanFilesForDirectory(String dir) throws IOException;
		
}
