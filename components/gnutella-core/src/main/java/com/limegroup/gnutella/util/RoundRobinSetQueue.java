
pbckage com.limegroup.gnutella.util;

import jbva.util.HashSet;
import jbva.util.Set;

/**
 * b Round Robin queue where elements are unique.
 */
public clbss RoundRobinSetQueue extends RoundRobinQueue {
	
	privbte Set _uniqueness;
	
	public RoundRobinSetQueue() {
		super();
		_uniqueness =  new HbshSet();
	}

	
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.util.RoundRobinQueue#enqueue(java.lang.Object)
	 */
	public synchronized void enqueue(Object vblue) {
		if (_uniqueness.bdd(value)) 
			super.enqueue(vblue);
		
	}
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.util.RoundRobinQueue#remove(java.lang.Object)
	 */
	public synchronized void remove(Object o) {
		if (_uniqueness.contbins(o)) {
			_uniqueness.remove(o);
			super.remove(o);
		}
		
	}
	/* (non-Jbvadoc)
	 * @see com.limegroup.gnutellb.util.RoundRobinQueue#removeAllOccurences(java.lang.Object)
	 */
	public synchronized void removeAllOccurences(Object o) {
		remove(o);
	}
}
