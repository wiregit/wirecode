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
	private final List QUERIES = new LinkedList();

	/**
	 * <tt>List</tt> of new queries to add.
	 * LOCKING: Thread-safe, although you must obtain a lock on NEW_QUERIES if
	 * it's ever iterated over.  
	 */
	private final List NEW_QUERIES = 
		Collections.synchronizedList(new LinkedList());

    /**
     * Variable for whether or not the last call to send out all pending 
     * queries has finished.
     */
    private static volatile boolean _done = true;

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
        Thread dispatcher = new Thread(this, "QueryDispatcher");
        dispatcher.setDaemon(true);
        dispatcher.start();
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
     * This method removes all queries for the given <tt>ReplyHandler</tt>
     * instance.
     *
     * @param handler the handler that should have it's queries removed
     */
    public void removeReplyHandler(ReplyHandler handler) {
        // if it's not a leaf connection, we don't care that it's closed
        if(!handler.isSupernodeClientConnection()) return;
        removeFromCollection(NEW_QUERIES, handler);
        removeFromCollection(QUERIES, handler);
    }

    /**
     * Removes the specified <tt>ReplyHandler</tt> from the specified
     * <tt>Collection</tt>.
     *
     * @param coll the <tt>Collection</tt> to remove the <tt>ReplyHandler</tt>
     *  from
     * @param handler the <tt>ReplyHandler</tt> to remove
     */
    private static void removeFromCollection(Collection coll, ReplyHandler handler) {
        List toRemove = new LinkedList();
        synchronized(coll) {
            Iterator iter = coll.iterator();
            while(iter.hasNext()) {
                QueryHandler qh = (QueryHandler)iter.next();
                ReplyHandler rh = qh.getReplyHandler();
                if(handler == rh) {
                    toRemove.add(qh);
                }
            }
            coll.removeAll(toRemove);
        }        
    }

	/**
	 * Starts the thread that processes queries.
	 */
	public void run() {
        try {
            while(true) {
                Thread.sleep(400);
                processQueries();
            }
        } catch(Throwable t) {
            ErrorService.error(t);
        }
	}

	/**
	 * Processes current queries.
	 */
	private void processQueries() {
        if(!_done) return;
        _done = false;

		// necessary to obtain the lock because addAll iterates over
		// NEW_QUERIES
		synchronized(NEW_QUERIES) {
			QUERIES.addAll(NEW_QUERIES);
			NEW_QUERIES.clear();
		}

        List expiredQueries = new LinkedList();

        synchronized(QUERIES) {
            Iterator iter = QUERIES.iterator();
            while(iter.hasNext()) {
                QueryHandler handler = (QueryHandler)iter.next();
                handler.sendQuery();
                if(handler.hasEnoughResults()) {
                    expiredQueries.add(handler);
                }
            }

            // remove any expired queries
            QUERIES.removeAll(expiredQueries);
        }
        _done = true;
	}
}



