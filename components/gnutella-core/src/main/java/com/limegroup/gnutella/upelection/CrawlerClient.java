/*
 * An interface that should be implemented by classes that want to receive 
 * notification about an ongoing asynchronous crawl 
 */
package com.limegroup.gnutella.upelection;

import com.sun.java.util.collections.Collection;

public interface CrawlerClient {
	
	/**
	 * the crawl has started.
	 */
	public void crawlStarted();
	
	/**
	 * the crawl has completed.  The results are in the Collection of Endpoints.
	 * @param results
	 */
	public void crawlFinished(Collection results);
}
