

package com.limegroup.gnutella.upelection;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;

/**
 * contains the current list of best candidates.  Singleton and thread-safe.
 * Although the leaf info is serializable, there is no need for this class
 * to be serializable as its data is valid only while Limewire is running.
 * 
 * The actual network de/serialization happens in BestCandidatesVendorMessage
 * 
 * Locking: obtain instance.
 */
public class BestCandidates {
	
	//index 0 is our best leaf,
	//index 1 is the best leaf at ttl 1 excluding our leaf
	//index 2 the best leaf at ttl 2 excluding ttl 1 and our leaf
	Candidate [] _best;
	
	
	private static BestCandidates instance = new BestCandidates();
	
	private BestCandidates() {
		_best = new Candidate[3];
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
		SortedSet s = new TreeSet(Candidate.priorityComparator());
		synchronized(instance) {
			s.addAll(Arrays.asList(instance._best));
		}
		return (Candidate) s.last();  //higher = better
		
	}
	
	/**
	 * updates the list of best candidates if necessary
	 * @param newCandidates the list of candidates received from some other
	 * node.  Null values means we don't have values for that ttl.
	 */
	public static void update(Candidate [] newCandidates) {
		Comparator comp = Candidate.priorityComparator();
		
		//if the other guy doesn't have a beast leaf, he shouldn't
		//have sent this message in the first place.  discard, regardless
		//whether he has a best leaf on ttl 1
		if (newCandidates[0]==null)
			return;
		
		synchronized(instance) {
			
			//compare my ttl 1 best with the other guy's ttl 0 best
			if (instance._best[1]==null ||
			 comp.compare(instance._best[1], newCandidates[0]) < 0)
				instance._best[1] = newCandidates[0];
			
			
			//and my ttl 2 best with the other guy's ttl 1 best
			
			if (newCandidates[1]==null)
				return; //he doesn't have one, retain mine
			
			if (instance._best[2]==null ||
			 comp.compare(instance._best[2], newCandidates[1]) < 0)
				instance._best[2] = newCandidates[1];
		}
	}
	
	/**
	 * updates my own best leaf.  Can be called as often as the best leaf 
	 * changes.
	 * @param myLeaf an ExtendedEndpoint representation of my new best leaf
	 */
	public static void update(Candidate myLeaf) {
		//no need to compare, just update
		//or maybe put some update flag here?
		synchronized(instance) {
			instance._best[0] = myLeaf;
		}
	}
	
	
}
