/*
 * A crawler which crawls out in a radial fashion - it attempts to find a set of peers which
 * are at a specific distance from the crawler and are separated from each other like this:
 *
 *X-hit, 0-skip, S-start
 * 
 *             X
 *            /|\
 *             0
 *             |
 * X<----0<----S---->0----->X
 *             |
 *             0
 *            \|/
 *             X
 */
 
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.Endpoint;
import com.sun.java.util.collections.Collection;
import com.sun.java.util.collections.Set;

//will do this guy tomorrow
public class RadialCrawler implements AsyncCrawler, BlacklistCrawler {
	
	public int getDegree() {
		return 0;
	}
	public void setDegree(int newDegree) {
		
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.AsyncCrawler#crawlAsync(com.limegroup.gnutella.upelection.CrawlerClient, int)
	 */
	public void crawlAsync(CrawlerClient callback, int depth) {
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.AsyncCrawler#crawlAsync(com.limegroup.gnutella.upelection.CrawlerClient)
	 */
	public void crawlAsync(CrawlerClient callback) {
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.AsyncCrawler#stopCrawl()
	 */
	public void stopCrawl() {
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.BlacklistCrawler#resetBlacklist()
	 */
	public void resetBlacklist() {
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.BlacklistCrawler#updateBlacklist(com.limegroup.gnutella.Endpoint)
	 */
	public void updateBlacklist(Endpoint node) {
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.BlacklistCrawler#updateBlacklist(com.sun.java.util.collections.Set)
	 */
	public void updateBlacklist(Set blacklist) {
		// TODO Auto-generated method stub
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.Crawler#crawl()
	 */
	public Collection crawl() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.Crawler#crawl(int)
	 */
	public Collection crawl(int ttl) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.Crawler#setDesiredResults(int)
	 */
	public void setDesiredResults(int number) {
		// TODO Auto-generated method stub
	}
}
