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
 * 
 * If the user has provided an initial BlackList, the crawl starts from those nodes.  
 * Otherwise the crawl starts from the directly connected ultrapeers.
 */
 
package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.*;

import com.sun.java.util.collections.*;


public class RadialCrawler implements AsyncCrawler, BlacklistCrawler {

	/**
	 * flag whether we are currently crawling or not.
	 */
	private volatile boolean _crawling = false;
	
	/**
	 * a dummy callback in case the crawler will be used in blocking mode
	 */
	private CrawlerClient _client;
	/**
	 * the breadth degree of the crawl.  -1 means all (32 in our case)
	 */
	private volatile int _degree;
	
	private static final int MAX_DEGREE=-1;
	
	private int _desiredTTL = Crawler.TTL;
	
	/**
	 * the desired number of results.  To be safe we try to fetch 1.5 times as many.
	 */
	private int _desiredResults = (int)(Crawler.DEFAULT_RESULTS * 1.5);
	
	/**
	 * the blacklisted nodes.  Synced because the user may add more nodes on-the-fly
	 */
	private Set _blackList = Collections.synchronizedSet(new HashSet());
	
	/**
	 * the results of the crawl.
	 */
	private Set _results = new HashSet();
	
	/**
	 * a second list of currently active guids.  Necessary for the stopCrawl().
	 */
	private Collection _registeredGuids = new LinkedList();
	
	/**
	 * this worker class listens for the udp pong containing the results
	 * of a specific UDP ping.  It updates the _blacklist set with the nodes which
	 * have already been crawled, and then it schedules more pings
	 * and registers other instances of itself to handle them.
	 * 
	 */
	private class CrawlerWorker implements MessageListener {
		
		/**
		 * the udp pong containing the list of ultrapeers
		 */
		private UPListVendorMessage _uplvm;
		
		/**
		 * the ttl of this specific worker.  When it reaches 0, the results will
		 * be added to the final set of results.
		 */
		private final int _ttl;
		
		
		/**
		 * creates a new instance with the specified ttl.
		 * @param ttl the depth this crawler should go on to.
		 */
		public CrawlerWorker(int ttl) {
			if (ttl < 0)
				throw new IllegalArgumentException("starting a worker with negative ttl == bad idea!");
			_ttl = ttl;
		}
		
		/**
		 * this method is called from the udp receiver thread - so it will be quick.
		 */
		public void processMessage(Message m) {
			
			//remove myself from both lists of listeners
			RouterService.getMessageRouter().unregisterMessageListener(new GUID(m.getGUID()));
			
			//check if the crawl has ended in the meantime
			if (!_crawling)
				return;
			
			//check if the crawler has enough results, if yes, stop.
			if (_results.size() >= _desiredResults)
				stopCrawl();
			
			//check if we got called with the proper type of message
			if (! (m instanceof UPListVendorMessage)) 
				throw new Error("peer responded with the wrong kind of message"); //TODO: decide what to do in this case
			
			_uplvm = (UPListVendorMessage)m;
		
			//first thing to do is remove ourselves from our own list of GUIDs
			_registeredGuids.remove(new GUID(_uplvm.getGUID()));
			
			//extract the results the remote host returned.
			List results = _uplvm.getUltrapeers();
			
			//1. Remove anybody already on the blacklist from the results.
			results.removeAll(_blackList);
			
			//2. Add our results to the blacklist so that other instances won't
			//crawl them.
			_blackList.addAll(results);
			
			//3. if this is our last ttl, add our results to the final results and return.
			if (_ttl <= 0) {
				_results.addAll(results);
				return;
			}
			
			//4. otherwise, schedule a ping to the people in our list
			//and register a listener for the corresponding pong.
			for (Iterator iter = results.iterator();iter.hasNext();) {
				
				//the target to crawl
				Endpoint target = (Endpoint)iter.next();
				
				//the guid to listen the pong for
				GUID key = new GUID(GUID.makeGuid());
				
				//the message to send (we do not request any leafs)
				GiveUPVendorMessage gupvm = new GiveUPVendorMessage(key,_degree,0);
				
				//register a new listener with ttl one less than ours
				RouterService.getMessageRouter().registerMessageListener(key,
						new CrawlerWorker(_ttl-1));
				
				//also add the guid to our own set of registered GUIDs
				_registeredGuids.add(key);
				
				//schedule the message to be sent.
				UDPService.instance().send(gupvm,target.getInetAddress(),target.getPort());
			}
			
		}
	}
	
	/**
	 * all crawls are async; this class simply emulates the sync crawls.
	 */
	private class DummyClient implements CrawlerClient {
		
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.upelection.CrawlerClient#crawlFinished(com.sun.java.util.collections.Collection)
		 */
		public synchronized void crawlFinished(Collection results) {
			// at this point the results will already be assigned
			// just in case compare them
			if (results.equals(_results));
			notifyAll();
		}
		
		/* (non-Javadoc)
		 * @see com.limegroup.gnutella.upelection.CrawlerClient#crawlStarted()
		 */
		public void crawlStarted() {
			// nothing
		}
}
	/**
	 * gets the degree of the crawler.  Shouldn't really be used other than for
	 * testing or for writing advanced crawlers
	 * @return the degree
	 */
	public int getDegree() {
		return _degree;
	}
	/**
	 * sets the degree of the crawler.  Shouldn't really be used other than for
	 * testing or for writing advanced crawlers
	 * @param newDegree the new degree.
	 */
	public void setDegree(int newDegree) {
		_degree = newDegree;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.AsyncCrawler#crawlAsync(com.limegroup.gnutella.upelection.CrawlerClient, int)
	 */
	public void crawlAsync(CrawlerClient callback, int depth) {
		_desiredTTL=depth;
		crawlAsync(callback);
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.AsyncCrawler#crawlAsync(com.limegroup.gnutella.upelection.CrawlerClient)
	 */
	public void crawlAsync(CrawlerClient callback) {
		
		_client = callback;
		//Unless the user has specified a degree of the radial crawl, we need to 
		//calculate it based on the number of desired results and the ttl.
		//degree = desired results ^ 1/ttl.  The default values produce a degree of 3:
		//48 results ^ 1/4 ttl = 2.65 => 3 degrees.
		if (_degree == 0) 
			_degree = (int)Math.round(Math.pow(_desiredResults,1/_desiredTTL));
		
		//then check if the user has provided entry points.  If not, use our own ultrapeers
		if (_blackList.size()==0)
			for (Iterator iter = RouterService.getConnectionManager().getConnectedGUESSUltrapeers().iterator();
				iter.hasNext();) {
				Connection c = (Connection)iter.next();
				_blackList.add(new Endpoint(c.getAddress(),c.getPort()));
			}
			
		//send the initial pings to these entry points on-thread
		//and schedule workers for their results
		for (Iterator iter = _blackList.iterator();iter.hasNext();) {
			
			Endpoint target = (Endpoint)iter.next();
			
			//create a new vendor message
			GUID key = new GUID(GUID.makeGuid());
			GiveUPVendorMessage gupvm = new GiveUPVendorMessage(key,_degree,0);
			
			//register on both lists
			_registeredGuids.add(key);
			RouterService.getMessageRouter().registerMessageListener(key,
					new CrawlerWorker(_desiredTTL));
			
			UDPService.instance().send(gupvm,target.getInetAddress(),target.getPort());
		}
		
		//notify the callback we have started the crawl
		_client.crawlStarted();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.AsyncCrawler#stopCrawl()
	 */
	public void stopCrawl() {
		
		// the simplest way to stop a crawl is to unregister all registered GUIDS.
		for (Iterator iter = _registeredGuids.iterator();iter.hasNext();) {
			GUID current = (GUID)iter.next();
			RouterService.getMessageRouter().unregisterMessageListener(current);
		}
		
		//reset the crawling flag
		_crawling= false;
		
		//notify any threads waiting in blocking crawl mode
		if (_client!=null) {
			synchronized(_client) {
				_client.notifyAll();
			}
			_client.crawlFinished(_results);
		}
		
		_client=null;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.BlacklistCrawler#resetBlacklist()
	 */
	public void resetBlacklist() {
		_blackList.clear();
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.BlacklistCrawler#updateBlacklist(com.limegroup.gnutella.Endpoint)
	 */
	public void updateBlacklist(Endpoint node) {
		_blackList.add(node);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.BlacklistCrawler#updateBlacklist(com.sun.java.util.collections.Set)
	 */
	public void updateBlacklist(Set blacklist) {
		_blackList.addAll(blacklist);
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.Crawler#crawl()
	 */
	public Collection crawl() throws Exception {
		return doBlockingCrawl();
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.Crawler#crawl(int)
	 */
	public Collection crawl(int ttl) throws Exception {
		_desiredTTL=ttl;
		return crawl();
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.upelection.Crawler#setDesiredResults(int)
	 */
	public void setDesiredResults(int number) {
		_desiredResults = (int)(1.5*number);
	}
	
	/**
	 * emulates a blocking crawl by creating a dummy client and waiting
	 * until it finishes
	 * @return Collection full of Endpoints if the crawl is successful
	 * @throws Exception if some exception happened on the crawling thread.
	 */
	private Collection doBlockingCrawl() throws Exception {
		
		if (_client!=null)
			throw new IllegalStateException("crawler already running.");
		
		_client = new DummyClient();
		crawlAsync(_client);
		synchronized(_client) {
			try {
				_client.wait();
			}catch (InterruptedException ignored) {  
				//ignore, just return the results.
			}
		}
		//at this point the crawl is finished or got interrupted
		
		//if we got here the crawl has been successful
		return _results;
	}
	
	
}
