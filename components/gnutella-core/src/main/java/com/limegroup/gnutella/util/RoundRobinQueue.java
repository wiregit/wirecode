package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.LinkedList;



/**
 * a round-robin queue.  basically two lists that flip back and forth.
 */
public class RoundRobinQueue  {

	LinkedList _current;
	
	
	/**
	 * do not create the terminating elements
	 */
	public RoundRobinQueue() {
		_current = new LinkedList();
		

	}
	
	/**
	 * enqueues the specified object in the round-robin queue.
	 * @param value the object to add to the queue
	 */
	public synchronized void enqueue(Object value) {
		
		_current.addLast(value);
		
	}
	
	/**
	 * @return the next object in the round robin queue
	 */
	public synchronized Object next() {
		Object ret = _current.removeFirst();
		_current.addLast(ret);
		return ret;
	}
	
	/**
	 * removes the next occurence of the specified object
	 * @param o the object to remove from the queue. 
	 */
	public synchronized void remove (Object o) {
		_current.remove(o);
	}
	
	/**
	 * removes all occurences of the given object in the list.
	 * @param o the object to remove.
	 */
	public synchronized void removeAllOccurences(Object o) {
		
		
		Iterator iterator = _current.iterator();
		while(iterator.hasNext())
			if (iterator.next().equals(o))
				iterator.remove();
			
	}
	
	public synchronized int size() {
		return _current.size();
	}
		
}
