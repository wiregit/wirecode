package com.limegroup.gnutella.search;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;

/**
 * This class handles the thread that dispatched dynamic queries for Ultrapeers.
 */
public final class QueryDispatcher implements Runnable {

	/**
	 * <tt>List</tt> of outstanding queries.  
	 */
	private List _queries = new LinkedList();

	/**
	 * <tt>List</tt> of new queries to add.
	 * LOCKING: Thread-safe, although you must obtain a lock on NEW_QUERIES if
	 * it's ever iterated over.  
	 */
	private final List NEW_QUERIES = 
		Collections.synchronizedList(new LinkedList());

	/**
	 * List of unexpired queries -- continually swapped with <tt>_queries</tt>.
	 */
	private final Set EXPIRED_QUERIES = new HashSet();

	/**
	 * <tt>QueryDispatcher</tt> instance following singleton.
	 */
	private static final QueryDispatcher INSTANCE = 
		new QueryDispatcher();
	
	
	/**
	 * Instance accessor for the <tt>QueryDispatcher</tt>.
	 *
	 * @return the <tt>QueryDispatcher</tt> instance
	 */
	public static QueryDispatcher instance() {
		return INSTANCE;
	}

	/**
	 * Creates a new <tt>QueryDispatcher</tt> instance -- private constructor
	 * ensures that no other classes can create this.
	 */
	private QueryDispatcher() {}

	/**
	 * Schudules the processing of queries for execution.
	 */
	public void start() {
		RouterService.schedule(this, 0, 400);
	}

	/**
	 * Adds the specified <tt>QueryHandler</tt> to the list of queries to
	 * process.
	 *
	 * @param handler the <tt>QueryHandler</tt> instance to add
	 */
	public void addQuery(QueryHandler handler) {
		NEW_QUERIES.add(handler);		   
	}

	/**
	 * Starts the thread that processes queries.
	 */
	public void run() {
		processQueries();
	}

	/**
	 * Processes current queries.
	 */
	private void processQueries() {
		// necessary to obtain the lock because addAll iterates over
		// NEW_QUERIES
		synchronized(NEW_QUERIES) {
			_queries.addAll(NEW_QUERIES);
			NEW_QUERIES.clear();
		}

		//System.out.println("QueryDispatcher::processQueries::got lock"); 
		Iterator iter = _queries.iterator();
		while(iter.hasNext()) {
			QueryHandler handler = (QueryHandler)iter.next();
			handler.sendQuery();
			if(handler.hasEnoughResults()) {
				EXPIRED_QUERIES.add(handler);
			}
		}

		// remove any expired queries
		_queries.removeAll(EXPIRED_QUERIES);
		EXPIRED_QUERIES.clear();
	}
}
