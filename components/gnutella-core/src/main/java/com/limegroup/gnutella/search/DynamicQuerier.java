package com.limegroup.gnutella.search;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;

/**
 * This class handles the thread that dispatched dynamic queries for Ultrapeers.
 */
public final class DynamicQuerier implements Runnable {

	/**
	 * List of outstanding queries.
	 */
	private List _queries = new LinkedList();

	private final Object QUERY_LOCK = new Object();

	/**
	 * List of unexpired queries -- continually swapped with <tt>_queries</tt>.
	 */
	private final Set EXPIRED_QUERIES = new HashSet();

	/**
	 * <tt>DynamicQuerier</tt> instance following singleton.
	 */
	private static final DynamicQuerier INSTANCE = 
		new DynamicQuerier();
	
	
	/**
	 * Instance accessor for the <tt>DynamicQuerier</tt>.
	 *
	 * @return the <tt>DynamicQuerier</tt> instance
	 */
	public static DynamicQuerier instance() {
		return INSTANCE;
	}

	/**
	 * Creates a new <tt>DynamicQuerier</tt> instance -- private constructor
	 * ensures that no other classes can create this.
	 */
	private DynamicQuerier() {}

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
		synchronized(QUERY_LOCK) {
			// this adds the query to the end of the list
			_queries.add(handler);		   
			System.out.println("DynamicQuerier::addQuery::size: "+_queries.size()); 
		}
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
		//System.out.println("DynamicQuerier::processQueries"); 
		synchronized(QUERY_LOCK) {
			//System.out.println("DynamicQuerier::processQueries::got lock"); 
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
}
