package com.nearskysolutions.cloudbackup.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component 
public class FileHandlerServiceImpl implements FileHandlerService {

	Logger logger = LoggerFactory.getLogger(FileHandlerServiceImpl.class);
	
	@Override
	public List<File> scanFilesForDirectory(String dir) throws IOException {
		List<File> files = new ArrayList<File>();
		
		if( null == dir ) {
			logger.error("Null directory argument passed to FileHandlerServiceImpl.scanFilesForDirectory");
			throw new NullPointerException("Directory name can't be null");
		} 

		File dirFile = new File(dir);
		
		if( false == dirFile.exists() || false == dirFile.isDirectory() ) {
			logger.error(String.format("File %s doesn't exist or is not a directory", dir));
			throw new IOException(String.format("File %s doesn't exist or is not a directory", dir));
		}
		
		collectFileList(dirFile, files);
		
		return files;
	}

	private void collectFileList(File dirFile, List<File> files) {

		logger.info(String.format("Scanning files for directory: %s", dirFile.getAbsolutePath()));
		
		List<File> childDirs = new ArrayList<File>();
		
		for(File f : dirFile.listFiles()) {
			if( f.isDirectory() ) {
				childDirs.add(f);
			} else {				
				logger.info(String.format("Adding file to scan list: %s", f.getAbsolutePath()));
				files.add(f);
			}
		}
		
		for(File dir : childDirs) {
			collectFileList(dir, files);
		}
	}

}
