/*
 * Performs the promotion process - the crawl, etc.
 */
package com.limegroup.gnutella.upelection;


import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.IpPort;



import java.io.*;

public class Promoter implements Runnable {
	
	final IpPort _target;
	
	public void run() {
		
		//first thing we do is promote ourselves to an ultrapeer.
		try {
			RouterService.getConnectionManager().becomeAnUPWithBackupConn(_target);
			
		}catch(IOException failed) {
			//we couldn't connect to the guy who asked us to become an UP.  Abort
			return;
		}
		
		//start the crawl (once implemented)
		
		
	}
	
	/**
	 * Thread that performs the promotion process, including crawling, etc.
	 * 
	 * @param target the UP that asked us to promote ourselves.
	 */
	public Promoter(IpPort target) {
		_target=target;
	}
	
	//override for test purposes
	//public void start() {
	//	ManagedRun();
	//}
}
