
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A RoundRobinQueue keeps a list of objects, and lets you loop through them endlessly.
 * When you call next(), you get the first object in the list, and the RoundRobinQueue moves it to the end.
 * 
 * A RoundRobinQueue looks like this:
 * 
 *            start           end
 * 
 * _current   a b c d e f g h i
 * 
 * It keeps a single list of objects, named _current.
 * enqueue(o) adds o to the end of the list, after i in the picture above.
 * next() returns the object at the start of the list, a, and then moves a to the end.
 */
public class RoundRobinQueue  {

	/**
	 * The list of objects this RoundRobinQueue keeps.
	 * 
	 * The name _current sounds like we have several lists, and this is the current one.
	 * This isn't the case.
	 * A RoundRobinQueue has a single list, this one, named _current.
	 */
	LinkedList _current;

	/**
	 * Make a new RoundRobinQueue.
	 * Its list starts out empty.
	 */
	public RoundRobinQueue() {

		// Make a new empty LinkedList and save it
		_current = new LinkedList();
	}

	/**
	 * Add an object to the end of the list.
	 * 
	 * @param value The Object to add to the end
	 */
	public synchronized void enqueue(Object value) {

		// Add the given Object to the end of our list
		_current.addLast(value);
	}

	/**
	 * Get the next object in this RoundRobinQueue.
	 * Returns a reference to the first object in our list.
	 * Moves that object to the end of our list.
	 * If you keep calling next(), you'll loop through all the objects in the list endlessly.
	 * 
	 * @return The next Object
	 */
	public synchronized Object next() {

		// Move the first object in our list to the end
		Object ret = _current.removeFirst();
		_current.addLast(ret);

		// Return a reference to that object
		return ret;
	}

	/**
	 * Remove a single instance of the given object from this list.
	 * 
	 * If there are 2 instances of the object in our list, calling remove() will remove the first one and leave the second one.
	 * Use removeAllOccurences() to get rid of them all.
	 * 
	 * @param o The object to remove from the list
	 */
	public synchronized void remove(Object o) {

		// Look for o in our list, and remove it the first place we find it
		_current.remove(o);
	}

	/**
	 * Remove a given object everywhere it appears in this RoundRobinQueue.
	 * 
	 * @param o The object to remove from the list
	 */
	public synchronized void removeAllOccurences(Object o) {

		// Loop for each object in our list
		Iterator iterator = _current.iterator();
		while (iterator.hasNext()) {

			// If this object is o, remove it
			if (iterator.next().equals(o)) iterator.remove();
		}
	}

	/**
	 * Find out how many objects are in the list this RoundRobinQueue keeps.
	 * 
	 * @return The number of objects we have
	 */
	public synchronized int size() {

		// Return the number of objects we have
		return _current.size();
	}

	/**
	 * Clear the list this RoundRobinQueue keeps.
	 * Removes all the objects from our list.
	 */
	public synchronized void clear() {

		// Clear our list
		_current.clear();
	}
}
