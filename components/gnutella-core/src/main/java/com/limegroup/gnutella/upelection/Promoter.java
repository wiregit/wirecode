/*
 * Performs the promotion process - the crawl, etc.
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.util.ManagedThread;


public class Promoter extends ManagedThread {
	public void ManagedRun() {
		throw new RuntimeException("implement!");
	}
	
	public Promoter(String name) {
		super(name);
	}
	
	//override to make sure its thrown.
	public void start() {
		throw new RuntimeException("implement!");
	}
}
