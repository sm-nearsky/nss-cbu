package com.nearskysolutions.cloudbackup.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.storage.blob.BlobOutputStream;

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
				
    	try{

    		fos = new FileOutputStream(destFile);
    		    	
    		logger.info(String.format("Starting zip for file/dir: %s, saving to dest file: %s, using ignore: %s", sourceFile.getAbsolutePath(), destFile.getAbsolutePath(), sourceDirIgnore));
    		
    		FileZipUtils.CreateZipOutputToStream(sourceFile, sourceDirIgnore, fos);
    		
    		logger.info(String.format("Zip complete for file/dir: ", sourceFile.getAbsolutePath()));
    		    		    	   
    	} finally {
    		    		
    		if( null != fos ) {
    			logger.info(String.format("Closing write to destination file: %s", destFile.getName()));
    			fos.close();
    		}    		
    	}
    	
    	logger.trace(String.format("Completing FileZipUtils.CreateZipFileOutput(File sourceFile, String sourceDirIgnore, File destFile): sourceFile - %s, sourceDirIgnore - %s, destFile - %s, ",
									sourceFile.getAbsolutePath(), sourceDirIgnore, destFile.getAbsolutePath()));
	}

	public static void CreateZipOutputToStream(File sourceFile, OutputStream outputStream) throws Exception {
		
		if( null == sourceFile || false == sourceFile.exists() ) { 
			throw new Exception("sourceFile reference null or doesn't exist");
		}
		
		if( null == outputStream ) { 
			throw new Exception("outputStream reference null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.CreateZipOutputToStream(File sourceFile, OutputStream outputStream): sourceFile - %s",
				sourceFile.getAbsolutePath()));
		
		logger.info(String.format("Creating zip to output stream from source file: %s", sourceFile.getAbsolutePath()));
		
		FileZipUtils.CreateZipOutputToStream(sourceFile, null, outputStream);
		
		logger.trace(String.format("Completing FileZipUtils.CreateZipOutputToStream(File sourceFile, OutputStream outputStream): sourceFile - %s",
				sourceFile.getAbsolutePath()));
	}

	public static void CreateZipOutputToStream(File sourceFile, String sourceDirIgnore, OutputStream outputStream) throws Exception {
		
		if( null == sourceFile || false == sourceFile.exists() ) { 
			throw new Exception("sourceFile reference null or doesn't exist");
		}
		
		if( null == outputStream ) { 
			throw new Exception("outputStream reference null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.CreateZipOutputToStream(File sourceFile, String sourceDirIgnore, OutputStream outputStream): sourceFile - %s, sourceDirIgnore - %s",
									sourceFile.getAbsolutePath(), sourceDirIgnore));
		
		ZipOutputStream zos = null;
				
    	try{
    		    		    	
    		logger.info(String.format("Starting zip for file/dir: %s, saving to output stream using source dir ignore: %s", sourceFile.getAbsolutePath(), sourceDirIgnore));
    		
    		zos = new ZipOutputStream(outputStream);
    		
    		logger.info(String.format("Creating zip to output stream from source file: %s, with sourceDirIgnore: %s", sourceFile.getAbsolutePath(), sourceDirIgnore));
    		
    		FileZipUtils.CreateZipOutputToZos(sourceFile, sourceDirIgnore, zos);
    		
    		logger.info(String.format("Zip complete for file/dir: ", sourceFile.getAbsolutePath()));
    		    		    	   
    	} finally {
    		
    		logger.info("Closing zip stream for provided output");
    		
    		if( null != zos ) {
    			zos.finish();
    		}
    	}
    	
    	logger.trace(String.format("Completing FileZipUtils.CreateZipOutputToStream(File sourceFile, String sourceDirIgnore, OutputStream outputStream): sourceFile - %s, sourceDirIgnore - %s",
									sourceFile.getAbsolutePath(), sourceDirIgnore));
	}
	
	public static void CreateZipOutputToStream(InputStream inputStream, OutputStream outputStream, String entryName) throws Exception {
		
		if( null == inputStream ) { 
			throw new Exception("inputStream reference null");
		}
		
		if( null == outputStream ) { 
			throw new Exception("outputStream reference null");
		}
		
		if( null == entryName ) { 
			throw new Exception("entryName reference null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.CreateZipOutputToStream(InputStream inputStream, OutputStream outputStream, String entryName): entryName: %s", entryName));
		
    	logger.info(String.format("Starting zip for input stream and saving to output stream with entry name: %s", entryName));
    		
    	ZipOutputStream zos = null;
    	
    	try
    	{    		
    		zos = new ZipOutputStream(outputStream);
    		
    		FileZipUtils.addToZipFile(inputStream, zos, entryName);
    		
    	} finally {
    		
    		logger.info("Closing zip stream for provided output");
    		
    		if( null != zos ) {
   				zos.finish();
    		}
    	}
    		
    	logger.info(String.format("Zip complete to output stream with entry name: %s", entryName));
    	    	
    	logger.trace(String.format("Completed FileZipUtils.CreateZipOutputToStream(InputStream inputStream, OutputStream outputStream, String entryName): entryName: %s", entryName));
	}

	public static void CreateCompositeZipArchive(Enumeration<ZipEntryHelper> zipEntryHandler, OutputStream outputStream) throws Exception {
		
		if( null == zipEntryHandler ) { 
			throw new Exception("zipEntryHandler reference can't be null");
		}
		
		if( null == outputStream ) { 
			throw new Exception("outputStream reference can't be null");
		}
		
		logger.trace("Starting FileZipUtils.CreateCompositeZipArchive(Enumeration<ZipEntryHelper> zipEntryHandler, OutputStream outputStream)");
		
		if( false == zipEntryHandler.hasMoreElements() ) {
			logger.info("No elements found in zip handler");
		} else {
			ZipOutputStream zos = null;
						
			try {
			
				zos = new ZipOutputStream(outputStream);
			
				while(zipEntryHandler.hasMoreElements()) {
					try(ZipEntryHelper helper = zipEntryHandler.nextElement()) {
						logger.info(String.format("Sending entry: %s to be saved in zip archive", helper.getEntryName()));

						FileZipUtils.addToZipFile(helper.getInputStream(), zos, helper.getEntryName());						
						
						logger.info("Completed append to zip archive");
					}
				}
				
				logger.info("Composite write complete to zip archive");
				
			} finally {
								
				if(null != zos) {
					zos.finish();											
				}
			}
		}				
    	
    	logger.trace("Completed FileZipUtils.CreateCompositeZipArchive(Enumeration<ZipEntryHelper> zipEntryHandler, OutputStream outputStream)");
	}

	private static void CreateZipOutputToZos(File sourceFile, String sourceDirIgnore, ZipOutputStream zos) throws Exception {
		
		if( null == sourceFile || false == sourceFile.exists() ) { 
			throw new Exception("sourceFile reference null or doesn't exist");
		}
		
		if( null == zos ) { 
			throw new Exception("outputStream reference null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.CreateZipOutputToZos(File sourceFile, String sourceDirIgnore, ZipOutputStream outputStream): sourceFile - %s, sourceDirIgnore - %s",
									sourceFile.getAbsolutePath(), sourceDirIgnore));
				
		if( false == sourceFile.isDirectory() ) {
				
			String sourceDirFinal;
			FileInputStream fin = null;
						
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
					
				fin = new FileInputStream(sourceFile);
				
				FileZipUtils.addToZipFile(fin, zos, entryName);
								
				logger.info(String.format("Completed save for file: %s to zip archive: %s", sourceFile.getName(), entryName));
				
			} finally {
				
				logger.info(String.format("Closing zip file for source: %s", sourceFile.getName()));
				
				if( null != fin ) {
					fin.close();
				}
								
				logger.info(String.format("Zip file close complete for for source: %s", sourceFile.getName()));
			}
			
		} else if( null != sourceFile.listFiles() && 0 < sourceFile.listFiles().length ) {
			
			logger.info(String.format("Processing files in directory: %s for zip archive", sourceFile.getName()));
			
			File[] childFiles = sourceFile.listFiles();
			
			for(int i = 0; i < childFiles.length; i++) {
				FileZipUtils.CreateZipOutputToZos(childFiles[i], sourceDirIgnore, zos);
			}
			
		} else {
			logger.info(String.format("No files found to process for zip archive in directory: %s", sourceFile.getName()));
		}	
		
    	
    	logger.trace(String.format("Completing FileZipUtils.CreateZipOutputToZos(File sourceFile, String sourceDirIgnore, OutputStream outputStream): sourceFile - %s, sourceDirIgnore - %s",
									sourceFile.getAbsolutePath(), sourceDirIgnore));
	}
	

	private static void addToZipFile(InputStream inputStream, ZipOutputStream zos, String entryName) throws Exception {
		
		logger.trace("Starting FileZipUtils.addToZipFile(InputStream inputStream, ZipOutputStream zos, String entryName)");
		
			
		int bufferSize = 4096;		
		byte[] buffer = new byte[bufferSize];
		int byteCount;
		ZipEntry ze = null;
				
		try {
			
			logger.info(String.format("Saving byte input to zip output as entry: %s", entryName));
				
			
			ze = new ZipEntry(entryName);
			zos.putNextEntry(ze);
								
			//TODO Note that Windows file attributes such as hidden and read only
			//     are not preserved using this method a different zip library would
			//     probably need to be used
			
			while (0 < (byteCount = inputStream.read(buffer, 0, bufferSize))) {
				zos.write(buffer, 0, byteCount);
			}
			
			logger.info(String.format("Completed save to zip entry: %s", entryName));
			
		} finally {
			
			logger.info(String.format("Closing zip entry: %s", entryName));
			
			if( null != ze ) {
				logger.info("Closing zip entry");
				zos.closeEntry();
			}
		}			
					
		logger.trace("Completing FileZipUtils.addToZipFile(InputStream inputStream, ZipOutputStream zos, String entryName)");
		
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
    		
    		WriteZipFileToOutput(sourceZipFile, fos);    		
    		
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
	
	public static void WriteZipFileToOutput(File sourceZipFile, OutputStream outputStream) throws Exception {
		
		if( null == sourceZipFile || false == sourceZipFile.exists() ) { 
			throw new Exception("sourceFile reference null or doesn't exist");
		}
		
		if( null == outputStream ) { 
			throw new Exception("ostream reference can't be null");
		}
		
		logger.trace(String.format("Starting FileZipUtils.WriteZipBytesToOutput(File sourceZipFile, OutputStream outputStream): sourceFile - %s",
									sourceZipFile.getAbsolutePath()));
				
		FileInputStream fis = null;
				
    	try{
    		    		    		
    		logger.info(String.format("Creating single file from zip archive: %s to output stream", 
    									sourceZipFile.getAbsolutePath()));
    		    		
    		fis = new FileInputStream(sourceZipFile);
    		
    		FileZipUtils.WriteZipInputToOutput(fis, outputStream);
    		
    		logger.info(String.format("Completing single file write from zip archive: %s to output stream", 
    									sourceZipFile.getName()));
    		
    	} finally {
    		      		    		
    		if( null != fis ) {
    			fis.close();
    		}
    	}
    	
    	logger.trace(String.format("Completing FileZipUtils.WriteZipBytesToOutput(File sourceZipFile, OutputStream outputStream): sourceFile - %s",
				sourceZipFile.getAbsolutePath()));
	}
	
	public static void WriteZipInputToOutput(InputStream inputStream, OutputStream outputStream) throws Exception {
		
		if( null == inputStream ) { 
			throw new Exception("inputStream reference can't be null");
		}
		
		if( null == outputStream ) { 
			throw new Exception("outputStream reference can't be null");
		}
		
		logger.trace("Starting FileZipUtils.WriteZipInputToOutput(InputStream inputStream, OutputStream outputStream)");
				
		ZipInputStream zis = null;
		ZipEntry ze = null;
		
    	try{
    		
    		int bufferSize = 4096;
    		byte[] buffer = new byte[bufferSize];
    		int byteCount;
    		
    		logger.info("Creating uncompressed output from zip input stream");
    		
    		zis = new ZipInputStream(inputStream);
    	    		
    		//Reading the entry is needed for the zip library to decompress the entry
    		ze = zis.getNextEntry();
    		
    		while (0 < (byteCount = zis.read(buffer, 0, bufferSize))) {
    			outputStream.write(buffer, 0, byteCount);
			}
    		
    		logger.info("Completed uncompressed output from zip input");
    		
    	} finally {
    		    		
    		if( null != ze ) {
    			zis.closeEntry();
    		}
    		
    		if( null != zis ) {
    			logger.info("Closing zip input stream");
    			
    			zis.close();
    		}
    	}
    	
    	logger.trace("Completed FileZipUtils.WriteZipInputToOutput(InputStream inputStream, OutputStream outputStream)");
	}
	
}
