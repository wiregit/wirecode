/*
 * Performs the promotion process - the crawl, etc.
 */
package com.limegroup.gnutella.upelection;


import com.limegroup.gnutella.*;



import java.io.*;

public class Promoter implements Runnable {
	
	final String _host;
	final int _port;
	
	public void run() {
		
		//first thing we do is promote ourselves to an ultrapeer.
		try {
			RouterService.getConnectionManager().becomeAnUPWithBackupConn(_host,_port);
			
		}catch(IOException failed) {
			//we couldn't connect to the guy who asked us to become an UP.  Abort
			return;
		}
		
		
		//throw new RuntimeException("keep implementing");
		//System.out.println("keep implementing");
		
	}
	
	/**
	 * Thread that performs the promotion process, including crawling, etc.
	 * 
	 * @param target the UP that asked us to promote ourselves.
	 */
	public Promoter(Endpoint target) {
		_host = target.getAddress();
		_port = target.getPort();
	}
	
	//override for test purposes
	//public void start() {
	//	ManagedRun();
	//}
}
