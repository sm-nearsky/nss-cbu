package com.nearskysolutions.cloudbackup.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileZipUtils {

	static Logger logger = LoggerFactory.getLogger(FileZipUtils.class);
	
	public static void CreateZipFileOutput(File sourceFile, File destFile) throws Exception {
		
		if( null == sourceFile || false == sourceFile.exists() ) { 
			throw new Exception("sourceFile reference null or doesn't exist");
		}
		
		if( null == destFile ) { 
			throw new Exception("destFile reference null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.CreateZipFileOutput(File sourceFile, File destFile): sourceFile - %s, destFile - %s, ",
				sourceFile.getAbsolutePath(), destFile.getAbsolutePath()));
		
		FileZipUtils.CreateZipFileOutput(sourceFile, null, destFile);
		
		logger.trace(String.format("Completing FileZipUtils.CreateZipFileOutput(File sourceFile, File destFile): sourceFile - %s, destFile - %s, ",
				sourceFile.getAbsolutePath(), destFile.getAbsolutePath()));
	}
	
	public static void CreateZipFileOutput(File sourceFile, String sourceDirIgnore, File destFile) throws Exception {
		
		if( null == sourceFile || false == sourceFile.exists() ) { 
			throw new Exception("sourceFile reference null or doesn't exist");
		}
		
		if( null == destFile ) { 
			throw new Exception("destFile reference null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.CreateZipFileOutput(File sourceFile, String sourceDirIgnore, File destFile): sourceFile - %s, sourceDirIgnore - %s, destFile - %s, ",
									sourceFile.getAbsolutePath(), sourceDirIgnore, destFile.getAbsolutePath()));
		
		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		
    	try{

    		fos = new FileOutputStream(destFile);
    		zos = new ZipOutputStream(fos);
    	
    		logger.info(String.format("Starting zip for file/dir: %s, saving to dest file: %s, using ignore: %s", sourceFile.getAbsolutePath(), destFile.getAbsolutePath(), sourceDirIgnore));
    		
    		addToZipFile(sourceFile, sourceDirIgnore, zos);
    		
    		logger.info(String.format("Zip complete for file/dir: ", sourceFile.getAbsolutePath()));
    		    		    	   
    	} finally {
    		if( null != zos ) {
    			logger.info("Closing write to zip output");
    			zos.close();
    		}
    		
    		if( null != fos ) {
    			logger.info(String.format("Closing write to destination file: %s", destFile.getName()));
    			fos.close();
    		}    		
    	}
    	
    	logger.trace(String.format("Completing FileZipUtils.CreateZipFileOutput(File sourceFile, String sourceDirIgnore, File destFile): sourceFile - %s, sourceDirIgnore - %s, destFile - %s, ",
									sourceFile.getAbsolutePath(), sourceDirIgnore, destFile.getAbsolutePath()));
	}


	private static void addToZipFile(File sourceFile, String sourceDirIgnore, ZipOutputStream zos) throws Exception {
		
		logger.trace(String.format("Starting FileZipUtils.addToZipFile(File sourceFile, String sourceDirIgnore, ZipOutputStream zos): sourceFile - %s, sourceDirIgnore - %s",
				sourceFile.getAbsolutePath(), sourceDirIgnore));
		
		if( false == sourceFile.isDirectory() ) {
						
			
			int bufferSize = 4096;		
			byte[] buffer = new byte[bufferSize];
			int byteCount;
			String sourceDirFinal;
			FileInputStream fin = null;
			ZipEntry ze = null;
			
			
			if( null == sourceDirIgnore ) {
				sourceDirFinal = sourceFile.getParent();
			} else {
				if( 0 != sourceFile.getParent().toLowerCase().indexOf(sourceDirIgnore.toLowerCase())) {
					throw new Exception("Directory name ignore substring not found");				
				} else {
					sourceDirFinal = sourceFile.getParent().substring(sourceDirIgnore.length());
				}
			}				
			
			String entryName = String.format("%s/%s", sourceDirFinal, sourceFile.getName());
			
			try {
				
				logger.info(String.format("Saving file: %s to zip archive: %s", sourceFile.getName(), entryName));
						
				ze = new ZipEntry(entryName);
				zos.putNextEntry(ze);
				fin = new FileInputStream(sourceFile);
						
				//TODO Note that Windows file attributes such as hidden and read only
				//     are not preserved using this method a different zip library would
				//     probably need to be used
				
				while (0 < (byteCount = fin.read(buffer, 0, bufferSize))) {
					zos.write(buffer, 0, byteCount);
				}
				
				logger.info(String.format("Completed save for file: %s to zip archive: %s", sourceFile.getName(), entryName));
				
			} finally {
				
				logger.info("Closing zip file for source: %s", sourceFile.getName());
				
				if( null != fin ) {
					fin.close();
				}
				
				if( null != ze ) {					
					zos.closeEntry();
				}
				
				logger.info("Zip file close complete for for source: %s", sourceFile.getName());
			}
			
		} else if( null != sourceFile.listFiles() && 0 < sourceFile.listFiles().length ) {
			
			logger.info(String.format("Processing files in directory: %s for zip archive", sourceFile.getName()));
			
			File[] childFiles = sourceFile.listFiles();
			
			for(int i = 0; i < childFiles.length; i++) {
				addToZipFile(childFiles[i], sourceDirIgnore, zos);
			}
			
		} else {
			logger.info(String.format("No files found to process for zip archive in directory: %s", sourceFile.getName()));
		}		
		
		logger.trace(String.format("Completing FileZipUtils.addToZipFile(File sourceFile, String sourceDirIgnore, ZipOutputStream zos): sourceFile - %s, sourceDirIgnore - %s",
				sourceFile.getAbsolutePath(), sourceDirIgnore));
		
	}	

	public static void CreateSingleFileFromZip(File sourceZipFile, File destFile) throws Exception {
		
		if( null == sourceZipFile || false == sourceZipFile.exists() ) { 
			throw new Exception("sourceFile reference null or doesn't exist");
		}
		
		if( null == destFile ) { 
			throw new Exception("destFile reference can't be null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.CreateSingleFileFromZip(File sourceZipFile, File destFile): sourceFile - %s, destFile - %s, ",
						sourceZipFile.getAbsolutePath(), destFile.getAbsolutePath()));
		
		FileOutputStream fos = null;
		
    	try{
    		
    		logger.info(String.format("Creating single file from zip archive: %s to destination file: %s", 
    									sourceZipFile.getAbsolutePath(), destFile.getAbsolutePath()));
    		
    		fos = new FileOutputStream(destFile);
    		
    		WriteZipBytesToOutput(sourceZipFile, fos);    		
    		
    		logger.info(String.format("Completing single file write from zip archive: %s to destination file: %s", 
    									sourceZipFile.getName(), destFile.getAbsolutePath()));
    		
    	} finally {    		    		
    		
    		if( null != fos ) {
    			logger.info(String.format("Closing output file: %s for zip archive: %s", destFile.getName(), sourceZipFile.getName()));
    			
    			fos.close();
    		}
    		
    	}
    	
    	logger.trace(String.format("Completing FileZipUtils.CreateSingleFileFromZip(File sourceZipFile, File destFile): sourceFile - %s, destFile - %s, ",
				sourceZipFile.getAbsolutePath(), destFile.getAbsolutePath()));
	}	
	
	public static void WriteZipBytesToOutput(File sourceZipFile, OutputStream ostream) throws Exception {
		
		if( null == sourceZipFile || false == sourceZipFile.exists() ) { 
			throw new Exception("sourceFile reference null or doesn't exist");
		}
		
		if( null == ostream ) { 
			throw new Exception("ostream reference can't be null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.WriteZipBytesToOutput(File sourceZipFile, OutputStream ostream): sourceFile - %s",
									sourceZipFile.getAbsolutePath()));
				
		FileInputStream fis = null;
		ZipInputStream zis = null;
		ZipEntry ze = null;
		
    	try{
    		
    		int bufferSize = 4096;
    		byte[] buffer = new byte[bufferSize];
    		int byteCount;
    		
    		logger.info(String.format("Creating single file from zip archive: %s to output stream", 
    									sourceZipFile.getAbsolutePath()));
    		    		
    		fis = new FileInputStream(sourceZipFile);
    		zis = new ZipInputStream(fis);
    	    		
    		//Reading the entry is needed for the zip library to decompress the entry
    		ze = zis.getNextEntry();
    		
    		while (0 < (byteCount = zis.read(buffer, 0, bufferSize))) {
    			ostream.write(buffer, 0, byteCount);
			}
    		
    		logger.info(String.format("Completing single file write from zip archive: %s to output stream", 
    									sourceZipFile.getName()));
    		
    	} finally {
    		    		
    		if( null != ze ) {
    			zis.closeEntry();
    		}
    		
    		if( null != zis ) {
    			logger.info(String.format("Closing zip input file: %s", sourceZipFile.getName()));
    			
    			zis.close();
    		}
    		
    		if( null != fis ) {
    			fis.close();
    		}
    	}
    	
    	logger.trace(String.format("Completing FileZipUtils.WriteZipBytesToOutput(File sourceZipFile, OutputStream ostream): sourceFile - %s",
				sourceZipFile.getAbsolutePath()));
	}
}
