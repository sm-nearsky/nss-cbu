package com.nearskysolutions.cloudbackup.util;

import com.nearskysolutions.cloudbackup.admin.AdminServicesServer;
import com.nearskysolutions.cloudbackup.client.CloudBackupClient;
import com.nearskysolutions.cloudbackup.server.CloudBackupServer;

public class CloudBackupStartup {

	private enum RunType {
		Admin,
		Server,
		Client,
		None
	}
	
	public static void main(String[] args) {
		
		try {
			RunType rt = RunType.None;
			
			String mode = System.getProperty("runMode");
			
			if( null != mode ) {
							
				if( mode.equalsIgnoreCase("admin") ) {
					rt = RunType.Admin;
				} else if( mode.equalsIgnoreCase("server") ) {
					rt = RunType.Server;
				} else if( mode.equalsIgnoreCase("client") ) {
					rt = RunType.Client;
				}				
			}		
			
			switch(rt) {
				case Admin:
					AdminServicesServer.main(args);			
					break;
					
				case Server:
					CloudBackupServer.main(args);
					break;
					
				case Client:
					CloudBackupClient.main(args);
					break;
					
				default:
				System.out.print("Missing or invalid run mode.  Use -DrunMode:Admin|Client|Server [processRestoreRequests]");
				break;
			}
		
		} catch(Exception ex) {		
			ex.printStackTrace();		
		}
	}
	
}
