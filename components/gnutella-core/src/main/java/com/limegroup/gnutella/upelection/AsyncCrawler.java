/*
 * Crawler that crawls in a separate thread.
 */
package com.limegroup.gnutella.upelection;



public interface AsyncCrawler extends Crawler {

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

	/**
	 * terminates an async crawl.
	 */
	public void stopCrawl();
}
