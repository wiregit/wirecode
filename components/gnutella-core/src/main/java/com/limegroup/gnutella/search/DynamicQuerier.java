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

	/**
	 * List of unexpired queries -- continually swapped with <tt>_queries</tt>.
	 */
	private final List UNEXPIRED_QUERIES = new LinkedList();

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
		RouterService.schedule(this, 1000*2, 1000);
	}

	/**
	 * Adds the specified <tt>QueryHandler</tt> to the list of queries to
	 * process.
	 *
	 * @param handler the <tt>QueryHandler</tt> instance to add
	 */
	public void addQuery(QueryHandler handler) {
		synchronized(_queries) {
			// this adds the query to the end of the list
			_queries.add(handler);
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
		synchronized(_queries) {
			Iterator iter = _queries.iterator();
			while(iter.hasNext()) {
				QueryHandler handler = (QueryHandler)iter.next();
				handler.sendQuery();
				if(!handler.hasEnoughResults()) {
					UNEXPIRED_QUERIES.add(handler);
				}
			}
			_queries = UNEXPIRED_QUERIES;
			UNEXPIRED_QUERIES.clear();
		}
	}
}
