/*
 * Performs the promotion process - the crawl, etc.
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;

import com.sun.java.util.collections.*;

import java.io.*;

public class Promoter extends ManagedThread {
	
	final String _host;
	final int _port;
	
	public void ManagedRun() {
		
		//first thing we do is promote ourselves to an ultrapeer.
		
		try {
			RouterService.getConnectionManager().becomeAnUPWithBackupConn(_host,_port);
		}catch(IOException failed) {
			//we couldn't connect to the guy who asked us to become an UP.  Abort 
			return;
		}
		
		throw new RuntimeException("keep implementing");
		
	}
	
	/**
	 * Thread that performs the promotion process, including crawling, etc.
	 * 
	 * @param host the host that requested our promotion.  We will connect to that host first
	 * @param port the port of the host that requested the promotion.
	 */
	public Promoter(String host, int port) {
		super("Promotion thread");
		_host = host;
		_port = port;
	}
	
	//override for test purposes
	public void start() {
		ManagedRun();
	}
}
