package com.limegroup.gnutella.util;

import java.util.*;



/**
 * a round-robin queue.  basically two lists that flip back and forth.
 */
public class RoundRobinQueue  {

	LinkedList _current,_other;
	Iterator _iterator;
	
	
	/**
	 * do not create the terminating elements
	 */
	public RoundRobinQueue() {
		_current = new LinkedList();
		_other = new LinkedList();
		
		_iterator = _current.iterator();
		

	}
	
	/**
	 * enqueues the specified object in the round-robin queue.
	 * @param value the object to add to the queue
	 */
	public synchronized void enqueue(Object value) {
		
		_other.addLast(value);
		
	}
	
	/**
	 * @return the next object in the round robin queue
	 */
	public synchronized Object next() {
		
		if (!_iterator.hasNext())
			switchQueues();
		
		if (!_iterator.hasNext())
			return null;
		
		Object ret =  _iterator.next();
		_iterator.remove();
		_other.add(ret);
		return ret;
	}
	
	private void switchQueues() {
		LinkedList temp = _current;
		_current = _other;
		_other = temp;
		_iterator = _current.iterator();
	}
	/**
	 * removes the next occurence of the specified object
	 * @param o the object to remove from the queue. 
	 */
	public synchronized void remove (Object o) {
		if (_current.contains(o)) {
			_current.remove(o);
			_iterator = _current.iterator();
		}
		
		if (_other.contains(o))
			_other.remove(o);
		
	}
	
	/**
	 * removes all occurences of the given object in the list.
	 * @param o the object to remove.
	 */
	public synchronized void removeAllOccurences(Object o) {
		
		//merge the lists for easier processing
		_current.addAll(_other);
		_other.clear();
		
		_iterator = _current.iterator();
		while(_iterator.hasNext())
			if (_iterator.next().equals(o))
				_iterator.remove();
			
		_iterator = _current.iterator();
	}
	
	public synchronized int size() {
		return _current.size() + _other.size();
	}
		
}
