package com.limegroup.gnutella.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableForSize;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.service.ErrorService;

import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ReplyHandler;

/**
 * Manages dynamic querying for Ultrapeers.
 *
 * This maintains the data for all active queries for this Ultrapeer and any
 * of its leaves, also providing an interface for removing active queries.
 * Queries may be removed, for example, when a leaf node with an active query
 * disconnects from the Ultrapeer.
 */
@Singleton
public final class QueryDispatcher implements Runnable {

	/**
	 * <tt>Map</tt> of outstanding queries.  
	 */
    @InspectableForSize("number of dispatched queries")
	private final Map<GUID, QueryHandler> QUERIES = new HashMap<GUID, QueryHandler>();

    /** Details about the queries */
    @InspectionPoint("dispatched queries details")
    public final Inspectable queryDetail = new Inspectable() {
        public Object inspect() {
            List<Object> l = new ArrayList<Object>(QUERIES.size());
            for(QueryHandler qh : QUERIES.values())
                l.add(qh.inspect());
            return l;
        }
    };
    
	/**
	 * <tt>List</tt> of new queries to add.
	 * LOCKING: Thread-safe, although you must obtain a lock on NEW_QUERIES if
	 * it's ever iterated over.  
	 */
    @InspectableForSize("number of newly dispatched queries")
	private final List<QueryHandler> NEW_QUERIES =
        Collections.synchronizedList(new LinkedList<QueryHandler>());
    
    /**
     * The ProcessingQueue that handles sending queries out.
     *
     * Items are added to this only if it's not already processing anything.
     */
    private final ExecutorService PROCESSOR = ExecutorsHelper.newProcessingQueue("QueryDispatcher");
    
    /**
     * Whether or not processing is already active.  If it is, we don't start it up again
     * when adding new queries.
     */
    @InspectablePrimitive("querydispatcher active")
    private boolean _active;
    
	/**
	 * Adds the specified <tt>QueryHandler</tt> to the list of queries to
	 * process.
	 *
	 * @param handler the <tt>QueryHandler</tt> instance to add
	 */
	public void addQuery(QueryHandler handler) {
        handler.sendQuery();  // immediately send out one query.
        synchronized(NEW_QUERIES) {
		    NEW_QUERIES.add(handler);
		    if(NEW_QUERIES.size() == 1 && !_active) {
		        _active = true;
		        PROCESSOR.execute(this);
            }
		}
	}

    /**
     * This method removes all queries for the given <tt>ReplyHandler</tt>
     * instance.
     *
     * @param handler the handler that should have it's queries removed
     */
    public void removeReplyHandler(ReplyHandler handler) {
        // if it's not a leaf connection, we don't care that it's closed
        if(!handler.isSupernodeClientConnection())
            return;
            
        remove(handler);
    }

    /** Updates the relevant QueryHandler with result stats from the leaf.
     */
    public void updateLeafResultsForQuery(GUID queryGUID, int numResults) {
        synchronized (QUERIES) {
            QueryHandler qh = QUERIES.get(queryGUID);
            if (qh != null)
                qh.updateLeafResults(numResults);
        }
    }

    /** Gets the number of results the Leaf has reported so far.
     *  @return a non-negative number if the guid exists, else -1.
     */
    public int getLeafResultsForQuery(GUID queryGUID) {
        synchronized (QUERIES) {
            QueryHandler qh = QUERIES.get(queryGUID);
            if (qh == null)
                return -1;
            else
                return qh.getNumResultsReportedByLeaf();
        }
    }

    /**
     * Removes all queries using the specified <tt>ReplyHandler</tt>
     * from NEW_QUERIES & QUERIES.
     *
     * @param handler the <tt>ReplyHandler</tt> to remove
     */
    private void remove(ReplyHandler handler) {
        synchronized(NEW_QUERIES) {
            Iterator<QueryHandler> iter = NEW_QUERIES.iterator();
            while(iter.hasNext()) {
                QueryHandler qh = iter.next();
                if(qh.getReplyHandler() == handler)
                    iter.remove();
            }
        }
        
        synchronized(QUERIES) {
            Iterator<QueryHandler> iter = QUERIES.values().iterator();
            while(iter.hasNext()) {
                QueryHandler qh = iter.next();
                if(qh.getReplyHandler() == handler)
                    iter.remove();
            }
        }
    }
    
    /**
     * Removes the specified <tt>ReplyHandler</tt> from NEW_QUERIES & QUERIES.
     *
     * @param handler the <tt>ReplyHandler</tt> to remove
     */
    private void remove(GUID guid) {
        synchronized(NEW_QUERIES) {
            Iterator<QueryHandler> iter = NEW_QUERIES.iterator();
            while(iter.hasNext()) {
                QueryHandler qh = iter.next();
                if(qh.getGUID().equals(guid))
                    iter.remove();
            }
        }
        
        synchronized(QUERIES) {
            Iterator<QueryHandler> iter = QUERIES.values().iterator();
            while(iter.hasNext()) {
                QueryHandler qh = iter.next();
                if(qh.getGUID().equals(guid))
                    iter.remove();
            }
        }
    }

	/**
	 * Processes queries until there is nothing left to process,
	 * or there are no new queries to process.
	 */
	public void run() {
        while(true) {
            try {
                Thread.sleep(400);
            } catch(InterruptedException ignored) {}
            
            try {
                // If there are no more queries to process...
                if(!processQueries()) {
                    synchronized(NEW_QUERIES) {
                        // If there are no new queries to add,
                        // set active to false & leave.
                        if(NEW_QUERIES.isEmpty()) {
                            _active = false;
                            return;
                        }
                        // else, loop.
                    }
                }
                // else, loop.
            } catch(Throwable t) {
                ErrorService.error(t);
            }
        }
	}

	/**
	 * Processes current queries.
	 */
	private boolean processQueries() {
		synchronized(NEW_QUERIES) {
            synchronized(QUERIES) {
                for(QueryHandler qh : NEW_QUERIES)
                    QUERIES.put(qh.getGUID(), qh);
            }
			NEW_QUERIES.clear();
		}

	    
        synchronized(QUERIES) {
            Iterator<QueryHandler> iter = QUERIES.values().iterator();
            while(iter.hasNext()) {
                QueryHandler handler = iter.next();
                handler.sendQuery();
                if(handler.hasEnoughResults())
                    iter.remove();
            }
            
            return !QUERIES.isEmpty();
        }
	}

    
    /**
     * Removes all queries that match this GUID.
     * 
     * @param g the <tt>GUID</tt> of the search to remove
     */
    public void addToRemove(GUID g) {
        remove(g);
    }
}



