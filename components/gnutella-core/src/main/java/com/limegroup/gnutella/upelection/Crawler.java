/*
 * Interface representing a crawler
 */
package com.limegroup.gnutella.upelection;

import com.sun.java.util.collections.Collection;

public interface Crawler {
	
	/**
	 * the default crawl depth
	 */
	public static final int TTL=4;
	
	/**
	 * starts a blocking crawl at the default depth.  
	 * @return Collection of Endpoints that represent the results of the crawl.
	 * @throws Exception something goes wrong with the crawl.
	 */
	public Collection crawl() throws Exception;
	
	/**
	 * starts a blocking crawl at the specified depth
	 * @param ttl the depth of the crawl in ttl  
	 * @return Collection of Endpoints that represent the results of the crawl.
	 * @throws Exception something goes wrong with the crawl.
	 */
	public Collection crawl(int ttl) throws Exception;
	
	/**
	 * starts an async crawl at default depth, notifying the provided callback for events
	 * @param callback the class that listens to the events.
	 */
	public void crawlAsync(CrawlerClient callback);
	
	/**
	 * starts an async crawl at the specified depth,  notifying the provided callback for events
	 * @param callback the class that listens to the events.
	 * @param depth the depth to do the crawl in ttl
	 */
	public void crawlAsync(CrawlerClient callback, int depth);
}
