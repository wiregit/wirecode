pbckage com.limegroup.gnutella.util;

import jbva.util.Iterator;
import jbva.util.LinkedList;



/**
 * b round-robin queue.  basically two lists that flip back and forth.
 */
public clbss RoundRobinQueue  {

	LinkedList _current;
	
	
	/**
	 * do not crebte the terminating elements
	 */
	public RoundRobinQueue() {
		_current = new LinkedList();
		

	}
	
	/**
	 * enqueues the specified object in the round-robin queue.
	 * @pbram value the object to add to the queue
	 */
	public synchronized void enqueue(Object vblue) {
		
		_current.bddLast(value);
		
	}
	
	/**
	 * @return the next object in the round robin queue
	 */
	public synchronized Object next() {
		Object ret = _current.removeFirst();
		_current.bddLast(ret);
		return ret;
	}
	
	/**
	 * removes the next occurence of the specified object
	 * @pbram o the object to remove from the queue. 
	 */
	public synchronized void remove (Object o) {
		_current.remove(o);
	}
	
	/**
	 * removes bll occurences of the given object in the list.
	 * @pbram o the object to remove.
	 */
	public synchronized void removeAllOccurences(Object o) {
		
		
		Iterbtor iterator = _current.iterator();
		while(iterbtor.hasNext())
			if (iterbtor.next().equals(o))
				iterbtor.remove();
			
	}
	
	public synchronized int size() {
		return _current.size();
	}
		
}
