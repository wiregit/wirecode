package com.limegroup.gnutella.search;

import java.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.ManagedThread;

/**
 * This class handles the thread that dispatches dynamic queries for Ultrapeers.
 * This maintains the data for all active queries for this Ultrapeer and any
 * of its leaves, also providing an interface for removing active queries.
 * Queries may be removed, for example, when a leaf node with an active query
 * disconnects from the Ultrapeer.
 */
public final class QueryDispatcher implements Runnable {

	/**
	 * <tt>Map</tt> of outstanding queries.  
	 */
	private final Map QUERIES = new HashMap(); // GUID -> QueryHandler

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
     * Constant <tt>Set</tt> of searches that the user has killed that should 
     * be removed.
	 */
    private final Set TO_REMOVE = Collections.synchronizedSet(new HashSet());
    

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
        Thread dispatcher = new ManagedThread(this, "QueryDispatcher");
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
        handler.sendQuery();  // immediately sent out one query.
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
        removeFromMap(QUERIES, handler);
    }

    /** Updates the relevant QueryHandler with result stats from the leaf.
     */
    public void updateLeafResultsForQuery(GUID queryGUID, int numResults) {
        synchronized (QUERIES) {
            QueryHandler qh = (QueryHandler) QUERIES.get(queryGUID);
            if (qh != null)
                qh.updateLeafResults(numResults);
        }
    }

    /** Gets the number of results the Leaf has reported so far.
     *  @return a non-negative number if the guid exists, else -1.
     */
    public int getLeafResultsForQuery(GUID queryGUID) {
        synchronized (QUERIES) {
            QueryHandler qh = (QueryHandler) QUERIES.get(queryGUID);
            if (qh == null) return -1;
            else return qh.getNumResultsReportedByLeaf();
        }
    }

    /**
     * Removes the specified <tt>ReplyHandler</tt> from the specified
     * <tt>Collection</tt>.
     *
     * @param coll the <tt>Collection</tt> to remove the <tt>ReplyHandler</tt>
     *  from
     * @param handler the <tt>ReplyHandler</tt> to remove
     */
    private static void removeFromCollection(Collection coll, 
		ReplyHandler handler) {
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
     * Removes the specified <tt>ReplyHandler</tt> from the specified
     * <tt>Map</tt>.
     *
     * @param map the <tt>Map</tt> to remove the <tt>ReplyHandler</tt>
     *  from
     * @param handler the <tt>ReplyHandler</tt> to remove
     */
    private static void removeFromMap(Map map, ReplyHandler handler) {
        List toRemove = new LinkedList();
        synchronized(map) {
            Iterator iter = map.entrySet().iterator();
            while(iter.hasNext()) {
                QueryHandler qh = 
                    (QueryHandler)((Map.Entry)iter.next()).getValue();
                if(qh.getReplyHandler() == handler)
                    toRemove.add(qh.getGUID());
            }


            iter = toRemove.iterator();
            while (iter.hasNext()) {
                map.remove((GUID)iter.next());
            }
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
            synchronized(QUERIES) {
                Iterator iter = NEW_QUERIES.iterator();
                while (iter.hasNext()) {
                    QueryHandler qh = (QueryHandler) iter.next();
                    QUERIES.put(qh.getGUID(), qh);
                }
            }
			NEW_QUERIES.clear();
		}

        List expiredQueries = new LinkedList();

        synchronized(QUERIES) {
            Iterator iter = QUERIES.entrySet().iterator();
            while(iter.hasNext()) {
                QueryHandler handler = 
                    (QueryHandler)((Map.Entry)iter.next()).getValue();
                
                if(TO_REMOVE.contains(handler.getGUID())) {
                    TO_REMOVE.remove(handler.getGUID());
                    expiredQueries.add(handler);
                }
                else
                    handler.sendQuery();

                if(handler.hasEnoughResults()) {
                    expiredQueries.add(handler);
                }
            }

            // remove any expired queries
            iter = expiredQueries.iterator();
            while (iter.hasNext()) {
                QueryHandler qh = (QueryHandler) iter.next();
                QUERIES.remove(qh.getGUID());
            }
        }
        _done = true;
	}

    
    /**
     * Adds the specified <tt>GUID</tt> to the group of searches to remove.  
     * This MUST only be called to remove searches for this node, such as when
     * the user cancels a search, and not for searches on behalf of other 
     * nodes.
     * 
     * @param g the <tt>GUID</tt> of the search to remove
     */
    public void addToRemove(GUID g) {
        TO_REMOVE.add(g);
    }
    
    

}



