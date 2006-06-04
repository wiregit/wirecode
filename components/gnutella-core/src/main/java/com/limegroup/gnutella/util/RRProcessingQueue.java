
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * An RRProcessingQueue keeps several lists of Runnable objects, and calls their run() methods and discards them, moving from list to list.
 * A RRProcessingQueue looks like this:
 * 
 * this   name1  name2  name3  name4
 * -----  -----  -----  -----  -----
 * a      f      i      m      n
 * b      g      j             o
 * c      h      k
 * d             l
 * e
 * 
 * Call invokeLater(h, name1) to add a new Runnable object to a named list.
 * This RRProcessingQueue has a thread that is calling run() on its objects and discarding them.
 * First, it will call a.run() and remove a from the first list.
 * Right after that, it will call f.run() and remove f from the name1 list.
 * The RRProcessingQueue's thread keeps going until it runs out of objects to run.
 * If a named list becomes empty, it removes the list.
 * 
 * The names of the lists are objects.
 * The default list uses the RRProcessingQueue itself as it's name, this.
 * To add a Runnable object to a RRProcessingQueue, call invokeLater(o, name4).
 * If you use invokeLater(e), the method will add it to the default, this, list.
 * 
 * RRProcessingQueue uses 3 classes: RoundRobinQueue, ProcessingQueue, and NamedQueue.
 * RoundRobinQueue gives RRProcessingQueue the ability to cycle through the lists.
 * Each list is a NamedQueue, which keeps Runnable objects.
 * RRProcessingQueue extends ProcessingQueue, gaining the single thread that calls the run() methods.
 */
public class RRProcessingQueue extends ProcessingQueue {

	/*
	 * A RRProcessingQueue has a HashMap named queues, and a RoundRobinQueue named lists.
	 * Both hold exactly the same NamedQueue objects.
	 * To look up a NamedQueue by its name, call queues.get(name).
	 * To loop through the NamedQueue objects endlessly, call lists.next().
	 */

	/**
	 * queues keeps all our lists of Runnable objects.
	 * Each queue is a NamedQueue, which is a list of Runnable objects and a name.
	 * 
	 * A HashMap maps keys to values.
	 * A key is an Object that is the name of a NamedQueue.
	 * The value is the NamedQueue.
	 */
	private final Map queues = new HashMap();

	/**
	 * The NamedQueue objects this RRProcessingQueue has.
	 * 
	 * A RoundRobinQueue keeps a list of objects.
	 * When you call next() on it, it returns a reference to the first one, and moves it to the end.
	 * You can keep calling next() to loop through the objects endlessly.
	 */
	private final RoundRobinQueue lists = new RoundRobinQueue();

	/** The total number of Runnable objects in all the named queues this RRProcessingQueue keeps. */
	private int size;

    /**
     * Make a new RRProcessingQueue which will make a new managed or regular thread with the given name and priority.
     * 
     * @param name     The name we'll give the thread we'll make to process the items
     * @param managed  True to make a LimeWire ManagedThread, false to just make a regular Java thread
     * @param priority The priority we'll run the thread at
     */
	public RRProcessingQueue(String name, boolean managed, int priority) {

		// Call the ProcessingQueue constructor
		super(name, managed, priority);
	}

    /**
     * Make a new RRProcessingQueue which will make a new managed or regular thread with the given name.
     * 
     * @param name    The name we'll give the thread we'll make to process the items
     * @param managed True to make a LimeWire ManagedThread, false to just make a regular Java thread
     */
	public RRProcessingQueue(String name, boolean managed) {

		// Call the ProcessingQueue constructor
		super(name, managed);
	}

    /**
     * Make a new ProcessingQueue which will make a new managed thread with the given name.
     * 
     * @param name The name we'll give the thread we'll make to process the items
     */
	public RRProcessingQueue(String name) {

		// Call the ProcessingQueue constructor
		super(name);
	}

	/**
	 * Give this RRProcessingQueue an object with a run() method for it to run.
	 * The RRProcessingQueue will have its thread call run() once, and then discard the object.
	 * 
	 * @param runner  The object with a run() method.
	 * @param queueId The name of the queue to put it in.
	 *                An RRProcessingQueue is like a ProcessingQueue, but keeps multiple lists of Runnable objects.
	 *                queueId is the name of the list to put the given runner in.
	 */
	public synchronized void invokeLater(Runnable runner, Object queueId) {

		// Get the NamedQueue we have for the given queueId name
		NamedQueue queue = (NamedQueue)queues.get(queueId);
		if (queue == null) {

			// We don't have a NamedQueue for queueId yet, make a new one
			queue = new NamedQueue(new LinkedList(), queueId); // Use a new empty LinkedList and name it queueId

			// Put the new NamedQueue we made in both queues and lists
			queues.put(queueId, queue); // Add it to our queues Map
			lists.enqueue(queue);
		}

		// Add the given Runnable object to the queue that has the matching queueId
		queue.list.add(runner);

		// Record we've got one more Runnable in this RRProcessingQueue
		size++;

		// Start our thread which will call the run() methods on Runnable objects, and then discard them
		notifyAndStart();
	}

	/**
	 * Give this RRProcessingQueue an object with a run() method for it to run.
	 * The RRProcessingQueue will have its thread call run() once, and then discard the object.
	 * 
	 * An RRProcessingQueue has many lists of Runnable objects.
	 * Each list is identified by an object that is its name.
	 * This invokeLater() method doesn't take a name object.
	 * It puts it in our list named by this object, sort of the default list.
	 * 
	 * @param r The object with a run() method.
	 */
	public synchronized void invokeLater(Runnable r) {

		// Call the other invokeLater() method
		invokeLater(r, this); // Give it this instead of an object as the name, put r in the default list
	}

	/**
	 * Determine if this RRProcessingQueue still has more objects to run.
	 * Tells if there are any more in any of its lists.
	 */
	protected synchronized boolean moreTasks() {

		// Return true if size, our count of Runnable objects in all of our lists, is more than 0
		return size > 0;
	}

	/**
	 * Get the next Runnable object from among our lists for our thread to run and discard.
	 * 
	 * This RRProcessingQueue might look like this:
	 * 
	 * name1  name2  name3
	 * -----  -----  -----
	 * a      b      c
	 * d      e      f
	 * 
	 * lists and queues both contain the 3 NamedQueue objects, with the names name1, name2, and name3.
	 * Each NamedQueue contains a list of Runnable objects, shown as the letters a through f.
	 * 
	 * This next() method moves to the next list.
	 * For instance, if it was previously on name1, it moves to name2.
	 * It removes and returns the first runnable in that list.
	 * So, calling next() repeatedly will return the objects in the order a, b, c, d, e, and f.
	 * If a named list runs out of objects, next() removes it from the RRProcessingQueue entirely.
	 * 
	 * @return The next object in this RRProcessingQueue its thread should call run.
	 *         null if this whole RRProcessingQueue is empty.
	 */
	protected synchronized Runnable next() {

		// Make a reference to the Runnable object from this RRProcessingQueue that we'll return
		Runnable ret = null;

		// Loop until we run out of NamedQueue lists
		while (lists.size() > 0) {

			// Point next at the list after the one we previously looked at
			NamedQueue next = (NamedQueue)lists.next();

			// Get a Runnable object from that list
			ret = next.next(); // Removes the object from the list, and returns it

			// We didn't get one because this list of Runnable objects is empty
			if (ret == null || next.list.isEmpty()) {

				// Remove the empty list from lists and queues
				lists.removeAllOccurences(next);
				queues.remove(next.name);
			}

			// If we got a Runnable object
			if (ret != null) {

				// next.next() above removed the object from the list, record that we have one less overall
				size--;

				// Return it
				return ret;
			}
		}

		// This whole RRProcessingQueue is empty
		return null;
	}

	/**
	 * Find the total number of Runnable objects in all the named queues this RRProcessingQueue keeps.
	 * 
	 * @return The number of objects we have
	 */
	public synchronized int size() {

		// Return the size we counted
		return size;
	}

	/**
	 * Clear all the contents of this RRProcessingQueue object.
	 */
	public synchronized void clear() {

		// Clear the queues and lists, which both had references to the same NamedQueue objects
		queues.clear();
		lists.clear();

		// Record that we don't have any Runnable objects in any of our lists
		size = 0;
	}

	/**
	 * Remove a NamedQueue this RRProcessingQueue has.
	 * Removes the one with a given name.
	 * 
	 * A RRProcessingQueue has keeps a list of NamedQueue objects.
	 * Each NamedQueue has a name.
	 * This method removes one.
	 * 
	 * @param name The name of the NamedQueue to remove from this RRProcessingQueue
	 */
	public synchronized void clear(Object name) {

		// Remove the NamedQueue we have with the given name
		NamedQueue toRemove = (NamedQueue)queues.remove(name);
		if (toRemove == null) return; // We don't have a NamedQueue with that name, so there is nothing for us to remove

		// Remove the NamedQueue from lists also, it was referenced by both queues and lists
		lists.removeAllOccurences(toRemove); // queues and lists both keep all of our NamedQueue objects

		// We're losing all the Runnable objects in it, decrement size by that number
		size -= toRemove.list.size(); // size is the total number of objects in all our NamedQueue objects
	}

	/** A NamedQueue keeps a list of Runnable objects, and has a name. */
	private class NamedQueue {

		/**
		 * A LinkedList for this NamedQueue to use.
		 * Code in the RRProcessingQueue class will put objects with run() methods in this list.
		 */
		final List list;

		/** The Object that is this NamedQueue's name. */
		final Object name;

		/**
		 * Make a new NamedQueue object.
		 * Only RRProcessingQueue.invokeLater() does this.
		 * 
		 * @param list A new empty LinkedList for this NamedQueue to use
		 * @param name An object to use as this new NamedQueue's name
		 */
		NamedQueue(List list, Object name) {

			// Save the given objects
			this.list = list;
			this.name = name;
		}

		/**
		 * Get the next Runnable object in the list this NamedQueue keeps.
		 * Removes the object from the member variable list, and returns it.
		 * 
		 * @return An object that implements the Runnable interface, and has a run() method.
		 *         null if this NamedQueue's list is empty.
		 */
		Runnable next() {

			// Return a Runnable object the RRProcessingQueue added to our list
			return
				list.isEmpty() ?          // If our list is empty
				null :                    // Return null, otherwise
				(Runnable)list.remove(0); // Remove the first object in the list, and return it
		}
	}
}
