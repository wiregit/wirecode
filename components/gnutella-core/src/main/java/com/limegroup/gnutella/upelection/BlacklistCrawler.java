/*
 * A crawler that avoids a list of nodes - the blacklist
 * 
 * These methods are intended for use with the async crawl.  If you
 * want to do a synchronous crawl, call these methods either from a
 * different thread or before starting the crawl.
 */
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.Endpoint;

import com.sun.java.util.collections.Set;

public interface BlacklistCrawler extends Crawler {
	
	/**
	 * adds the Set of endpoints to the blacklist
	 * @param blacklist Set of endpoints to avoid
	 */
	public void updateBlacklist(Set blacklist);
	
	/**
	 * adds the specified Endpoint to the black list
	 * @param node the Endpoint to avoid
	 */
	public void updateBlacklist(Endpoint node);
	
	/**
	 * resets the blacklist
	 */
	public void resetBlacklist();
}
