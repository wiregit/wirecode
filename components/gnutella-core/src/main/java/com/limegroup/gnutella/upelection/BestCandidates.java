

package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.vendor.BestCandidatesVendorMessage;
import com.limegroup.gnutella.util.IpPort;
import com.sun.java.util.collections.*;


/**
 * contains the current list of best candidates.  Singleton and thread-safe.
 * Although the leaf info is serializable, there is no need for this class
 * to be serializable as its data is valid only while Limewire is running.
 * The actual network de/serialization happens in BestCandidatesVendorMessage
 * 
 * A leaf can be present more than once if it is connected to more than one
 * advertising ultrapeer.  In that case the getBest() method will return the closest
 * route to the leaf.
 * 
 * Locking: obtain instance.
 */
public class BestCandidates {
	
	//index 0 is our best leaf,
	//index 1 is the best leaf at ttl 1 excluding our leaf
	//index 2 the best leaf at ttl 2 excluding ttl 1 and our leaf
	Candidate [] _best;
	
	static CandidateAdvertiser _advertiser;
	
	/**
	 * a comparator to compare the leaves for potential candidates
	 */
	static LeafComparator _leafComparator = new LeafComparator();
	
	/**
	 * a comparator to use on already selected candidates.
	 */
	static CandidatePriorityComparator _candidateComparator = new CandidatePriorityComparator();
	
	private static BestCandidates instance = new BestCandidates();
	
	private BestCandidates() {
		_best = new Candidate[3];
		
		if(_advertiser!=null)
			_advertiser.interrupt();
		
		_advertiser = new CandidateAdvertiser();
		_advertiser.setDaemon(true);
		_advertiser.setName("candidate advertiser");
		_advertiser.start();
	}
	
	/**
	 * @return the list of the three candidates.
	 */
	public static Candidate [] getCandidates() {
		synchronized(instance) {
			return instance._best;
		}
	}
	
	
	/**
	 * @return the absolutely best candidate at all hops
	 */
	public static Candidate getBest() {
		Comparator comparator = RemoteCandidate.priorityComparator();
		Candidate best = instance._best[0];
		synchronized(instance) {
			if (comparator.compare(instance._best[1],best) > 0)
				best = instance._best[1];
			if (comparator.compare(instance._best[2],best) > 0)
				best = instance._best[2];
		}
		return best;
	}
	
	/**
	 * updates the list of best candidates if necessary
	 * @param newCandidates the list of candidates received from some other
	 * node.  Null values means we don't have values for that ttl.
	 */
	public static void update(Candidate [] newCandidates) {
			
		Comparator comp = new CandidatePriorityComparator();
		
		//if the other guy doesn't have a best leaf, he shouldn't
		//have sent this message in the first place.  discard, regardless
		//whether he has a best leaf on ttl 1
		if (newCandidates[0]==null)
			return;
		
		synchronized(instance) {
			
			//compare my ttl 1 best with the other guy's ttl 0 best
			//if mine is null, take his candidate
			//do the same if his candidate is better
			//or he is changing his mind about his best candidate
			if (instance._best[1]==null || 
					comp.compare(instance._best[1], newCandidates[0]) < 0 ||  
							instance._best[1].getAdvertiser().isSame(newCandidates[0].getAdvertiser()))  
				instance._best[1] = newCandidates[0];
			
			
			//and my ttl 2 best with the other guy's ttl 1 best
			
			if (newCandidates[1]==null){
				propagateChange();
				return; //he doesn't have one, retain mine
			}
			
			if (instance._best[2]==null ||
			 comp.compare(instance._best[2], newCandidates[1]) < 0 ||
			 	newCandidates[1].getAdvertiser().isSame(instance._best[2].getAdvertiser()))
					instance._best[2] = newCandidates[1];
			
			propagateChange();
		}
	}
	
	
	/**
	 * cleans up the table of the candidates whose route
	 * has failed.
	 * @param ip an <tt>IpPort</tt> that is no longer up.
	 */
	public static void routeFailed(IpPort ip) {
		
		//this method will be called every time a connection
		//is removed from the ConnectionManager, so the first
		//check needs to be unlocked.
		
		for (int i=0;i<3;i++)
			if (instance._best[i]!=null &&
					instance._best[i].getAdvertiser().isSame(ip)) 
				synchronized(instance) {
					instance._best[i]=electBest(i);
					propagateChange();
				}
	}
	
	/**
	 * sets the current message to be sent in the next awakening of the advertising
	 * thread.  We must have a best candidate of our own before we start propagating
	 * other people's.
	 */
	private static void propagateChange(){
		if (instance._best[0]!=null) 
			_advertiser.setMsg(new BestCandidatesVendorMessage(instance._best));
		
	} 
	
	/**
	 * goes through our currnetly known candidates and selects
	 * the best one at given ttl.
	 * LOCKING: make sure you hold instance.
	 * @param ttl the ttl of the candidate that needs election. 
	 * valid values are 0, 1 and 2
	 * @return the best candidate amongst the advertised connections.
	 */
	private static Candidate electBest(int ttl) {

		Candidate best = null;
		
		//if we are electing at ttl 0, cycle through the leaves.
		if (ttl == 0)
			for (Iterator iter = RouterService.getConnectionManager().
				getInitializedClientConnections().iterator();iter.hasNext();) {
			
				ManagedConnection current = (ManagedConnection)iter.next();
				
				if (current.remoteHostSupportsBestCandidates() >=1 &&
						current.isGoodCandidate() &&
						_leafComparator.compare(current,best) > 0)
						//get the best connection.	
							best = current;				
			}
		
		else 
			//we are electing amongst the candidates our connections 
			//advertised - cycle through ultrapeers.
			
			for (Iterator iter = RouterService.getConnectionManager().
					getInitializedConnections().iterator();iter.hasNext();) {
				
				Connection current = (Connection)iter.next();
				
				Candidate [] candidates = current.getCandidates();
				
				if (candidates == null)
					continue;
				
				if (_candidateComparator.compare(candidates[ttl-1],best) >0)
					best = candidates[ttl-1];
			}
		
		return best;
	}
	
	/**
	 * purges the table of best candidates.
	 * useful when disconnecting.
	 * 
	 * WARNING: calling this method will delay the next advertisement of
	 * candidates for another CandidateAdvertiser.INITIAL_DELAY.
	 */
	public static void purge() {
		instance = new BestCandidates();
	}
	
	/**
	 * initializes the table with our best candidate.
	 */
	public static void initialize() {
		synchronized(instance) {
			instance._best[0]=electBest(0);
			propagateChange();
		}
	}
	
	/**
	 * Finds a connection on which a specific message should be forwarded to.
	 * up to certain ttl away.
	 * 
	 * @param c the IpPort that we wish to get the route for
	 * @param ttl look for candidates no farther than that ttl
	 * @return the <tt>Connection</tt> that we should route the request on
	 */
	public static Connection getRoute(IpPort c, int ttl) {
		
		if (ttl<0) return null;
		
		synchronized(instance){
			if (instance._best==null)
				return null;
			
			int size = Math.min(ttl, instance._best.length-1);
			
			for (int i =0;i<=size;i++)
				if (c.isSame(instance._best[i]))
					return instance._best[i].getAdvertiser();
		}
		return null;
	}
	
	
}
