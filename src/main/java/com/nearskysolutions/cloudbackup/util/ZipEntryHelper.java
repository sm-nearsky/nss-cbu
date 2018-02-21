package com.nearskysolutions.cloudbackup.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class ZipEntryHelper implements Closeable {

	private String entryName = null;	
	private InputStream inputStream = null;	
	private boolean isInputZipped = false;
	private ZipInputStream zipInputStream = null;
	
	public ZipEntryHelper(String entryName, 
						  InputStream inputStream,
						  boolean isInputZipped) {
		this.entryName = entryName;
		this.inputStream = inputStream;		
		this.isInputZipped = isInputZipped;
	}
	
	public String getEntryName() {
		return entryName;
	}

	public void setEntryName(String entryName) {
		this.entryName = entryName;
	}

	public InputStream getInputStream() throws Exception {
		InputStream retVal = inputStream;
		
		if( this.isInputZipped ) {			
			this.zipInputStream = new ZipInputStream(retVal);
			this.zipInputStream.getNextEntry();
			
			retVal = this.zipInputStream;
		}
		
		return retVal;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public boolean isInputZipped() {
		return isInputZipped;
	}

	public void setInputZipped(boolean isInputZipped) {
		this.isInputZipped = isInputZipped;
	}

	@Override
	public void close() throws IOException {
		if( null != zipInputStream ) {
			zipInputStream.closeEntry();
		}
		
		if( null != inputStream ) {			
			inputStream.close();
		}
		
	}	
}
