
package com.limegroup.gnutella.util;

import java.util.HashSet;
import java.util.Set;

/**
 * a Round Robin queue where elements are unique.
 */
public class RoundRobinSetQueue extends RoundRobinQueue {
	
	private Set _uniqueness;
	
	public RoundRobinSetQueue() {
		super();
		_uniqueness =  new HashSet();
	}

	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#enqueue(java.lang.Object)
	 */
	public synchronized void enqueue(Object value) {
		if (_uniqueness.add(value)) 
			super.enqueue(value);
		
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#remove(java.lang.Object)
	 */
	public synchronized void remove(Object o) {
		if (_uniqueness.contains(o)) {
			_uniqueness.remove(o);
			super.remove(o);
		}
		
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.RoundRobinQueue#removeAllOccurences(java.lang.Object)
	 */
	public synchronized void removeAllOccurences(Object o) {
		remove(o);
	}
}
