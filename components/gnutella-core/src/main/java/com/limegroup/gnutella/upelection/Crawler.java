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
	 * the number of desired UP results
	 */
	public static final int MAX_RESULTS=32;
	
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
	 * sets the number of desired crawl results.  If called asynchronously 
	 * and the crawl has already generated enough results the crawl will end.
	 * @param number the number of desired results.
	 */
	public void setDesiredResults(int number);
}
