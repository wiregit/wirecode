

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
 * Locking: obtain instance or this.
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
	
	/**
	 * 
	 * @return an instance method. 
	 */
	public static BestCandidates instance() {
		return instance;
	}
	
	protected BestCandidates() {
		_best = new Candidate[3];
		
		if(_advertiser!=null)
			_advertiser.cancel();
		
		_advertiser = new CandidateAdvertiser(true);

		
	}
	
	/**
	 * @return a copy of the list of the three candidates.
	 */
	public Candidate [] getCandidates() {
		return copyCandidates(true);
	}
	
	
	/**
	 * @return the absolutely best candidate at all hops
	 */
	public Candidate getBest() {
		Comparator comparator = RemoteCandidate.priorityComparator();
		
		Candidate [] copy = copyCandidates(false);
		
		Candidate best = copy[0];
		
		if (comparator.compare(copy[1],best) > 0)
			best = copy[1];
		if (comparator.compare(copy[2],best) > 0)
			best = copy[2];
		
		return best;
	}
	
	/**
	 * updates the list of best candidates if necessary
	 * @param newCandidates the list of candidates received from some other
	 * node.  Null values means we don't have values for that ttl.
	 */
	public void update(Candidate [] newCandidates) {
			
		Comparator comp = new CandidatePriorityComparator();
		
		//if the other guy doesn't have a best leaf, he shouldn't
		//have sent this message in the first place.  discard, regardless
		//whether he has a best leaf on ttl 1
		
		
		Candidate []newBest = copyCandidates(false);
			
		//compare my ttl 1 best with the other guy's ttl 0 best
		//if mine is null, take his candidate
		//do the same if his candidate is better
		//or he is changing his mind about his best candidate
		if (newCandidates[0]!=null)
			if (newBest[1]==null || 
				comp.compare(newBest[1], newCandidates[0]) < 0 ||  
						newBest[1].getAdvertiser().isSame(newCandidates[0].getAdvertiser()))  
			newBest[1] = newCandidates[0];
			
			
		//and my ttl 2 best with the other guy's ttl 1 best
		if (newCandidates[1]!=null)	
			if (newBest[2]==null ||
				 comp.compare(newBest[2], newCandidates[1]) < 0 ||
				 	newBest[2].getAdvertiser().isSame(newCandidates[1].getAdvertiser()))
			newBest[2] = newCandidates[1];
		
		synchronized(this) {
			_best = newBest;
		}
		
		propagateChange();
		
	}
	
	
	/**
	 * cleans up the table of the candidates whose route
	 * has failed and propagates any changes
	 * @param ip an <tt>IpPort</tt> that is no longer up.
	 */
	public void routeFailed(IpPort ip) {
		
		//this method will be called every time a connection
		//is removed from the ConnectionManager, so the first
		//check needs to be unlocked.
		
		Candidate [] copy = copyCandidates(false);
		
		boolean update = false;
		
		for (int i=0;i<copy.length;i++)
			if (copy[i]!=null &&
					copy[i].getAdvertiser().isSame(ip)) { 
					copy[i]=electBest(i);
					update=true;
				}
		
		if (update) {
			synchronized(this) {
				_best=copy;
			}
			propagateChange();
		}
		
	}
	
	/**
	 * sets the current message to be sent in the next awakening of the advertising
	 * thread.  
	 */
	private synchronized void propagateChange(){
		if (_best[0]==null && _best[1]==null)
			return;
		_advertiser.setMsg(new BestCandidatesVendorMessage(_best));
	} 
	
	/**
	 * goes through our currnetly known candidates and selects
	 * the best one at given ttl.
	 * LOCKING: make sure you hold instance.
	 * @param ttl the ttl of the candidate that needs election. 
	 * valid values are 0, 1 and 2
	 * @return the best candidate amongst the advertised connections.
	 */
	private Candidate electBest(int ttl) {

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
	 */
	public synchronized void purge() {
		_best = new Candidate[3];
	}
	
	/**
	 * initializes the table with our best candidate.
	 */
	public void initialize() {
		
		Candidate best = electBest(0);
		
		synchronized(this) {
			_best[0]=best;
		}
		
		propagateChange();	
	}
	
	/**
	 * Finds a connection on which a specific message should be forwarded to.
	 * up to certain ttl away.
	 * 
	 * @param c the IpPort that we wish to get the route for
	 * @param ttl look for candidates no farther than that ttl
	 * @return the <tt>Connection</tt> that we should route the request on
	 */
	public synchronized Connection getRoute(IpPort c, int ttl) {
		
		if (ttl<0) return null;
		
		
		if (_best==null)
			return null;
			
		int size = Math.min(ttl, _best.length-1);
			
		for (int i =0;i<=size;i++)
			if (c.isSame(_best[i]))
				return _best[i].getAdvertiser();
		
		return null;
	}
	
	/**
	 * method which can be used to get a copy of the candidates table
	 * @param lock whether to lock the object when making a copy.  May
	 * not be always necessary
	 * @return a copy of the candidates table
	 */
	private final Candidate[] copyCandidates(boolean lock) {
		Candidate []ret = new Candidate[_best.length];
		
		if (lock)
			synchronized(this) {
				for (int i = 0;i<ret.length;i++)
					ret[i]=_best[i];
			}
		else
			for (int i = 0;i<ret.length;i++)
				ret[i]=_best[i];
		
		return ret;
	}
	
	
}
