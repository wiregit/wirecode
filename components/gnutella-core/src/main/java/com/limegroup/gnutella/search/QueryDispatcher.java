pbckage com.limegroup.gnutella.search;

import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Map;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.ReplyHandler;
import com.limegroup.gnutellb.util.ProcessingQueue;

/**
 * Mbnages dynamic querying for Ultrapeers.
 *
 * This mbintains the data for all active queries for this Ultrapeer and any
 * of its lebves, also providing an interface for removing active queries.
 * Queries mby be removed, for example, when a leaf node with an active query
 * disconnects from the Ultrbpeer.
 */
public finbl class QueryDispatcher implements Runnable {

	/**
	 * <tt>Mbp</tt> of outstanding queries.  
	 */
	privbte final Map QUERIES = new HashMap(); // GUID -> QueryHandler

	/**
	 * <tt>List</tt> of new queries to bdd.
	 * LOCKING: Threbd-safe, although you must obtain a lock on NEW_QUERIES if
	 * it's ever iterbted over.  
	 */
	privbte final List NEW_QUERIES = Collections.synchronizedList(new LinkedList());

	/**
	 * <tt>QueryDispbtcher</tt> instance following singleton.
	 */
	privbte static final QueryDispatcher INSTANCE = new QueryDispatcher();
    
    /**
     * The ProcessingQueue thbt handles sending queries out.
     *
     * Items bre added to this only if it's not already processing anything.
     */
    privbte final ProcessingQueue PROCESSOR = new ProcessingQueue("QueryDispatcher");
    
    /**
     * Whether or not processing is blready active.  If it is, we don't start it up again
     * when bdding new queries.
     */
    privbte boolean _active;
    

	/**
	 * Instbnce accessor for the <tt>QueryDispatcher</tt>.
	 *
	 * @return the <tt>QueryDispbtcher</tt> instance
	 */
	public stbtic QueryDispatcher instance() {
		return INSTANCE;
	}

	/**
	 * Crebtes a new <tt>QueryDispatcher</tt> instance.
	 */
	privbte QueryDispatcher() {}

	/**
	 * Adds the specified <tt>QueryHbndler</tt> to the list of queries to
	 * process.
	 *
	 * @pbram handler the <tt>QueryHandler</tt> instance to add
	 */
	public void bddQuery(QueryHandler handler) {
        hbndler.sendQuery();  // immediately send out one query.
        synchronized(NEW_QUERIES) {
		    NEW_QUERIES.bdd(handler);
		    if(NEW_QUERIES.size() == 1 && !_bctive) {
		        _bctive = true;
		        PROCESSOR.bdd(this);
            }
		}
	}

    /**
     * This method removes bll queries for the given <tt>ReplyHandler</tt>
     * instbnce.
     *
     * @pbram handler the handler that should have it's queries removed
     */
    public void removeReplyHbndler(ReplyHandler handler) {
        // if it's not b leaf connection, we don't care that it's closed
        if(!hbndler.isSupernodeClientConnection())
            return;
            
        remove(hbndler);
    }

    /** Updbtes the relevant QueryHandler with result stats from the leaf.
     */
    public void updbteLeafResultsForQuery(GUID queryGUID, int numResults) {
        synchronized (QUERIES) {
            QueryHbndler qh = (QueryHandler) QUERIES.get(queryGUID);
            if (qh != null)
                qh.updbteLeafResults(numResults);
        }
    }

    /** Gets the number of results the Lebf has reported so far.
     *  @return b non-negative number if the guid exists, else -1.
     */
    public int getLebfResultsForQuery(GUID queryGUID) {
        synchronized (QUERIES) {
            QueryHbndler qh = (QueryHandler) QUERIES.get(queryGUID);
            if (qh == null)
                return -1;
            else
                return qh.getNumResultsReportedByLebf();
        }
    }

    /**
     * Removes bll queries using the specified <tt>ReplyHandler</tt>
     * from NEW_QUERIES & QUERIES.
     *
     * @pbram handler the <tt>ReplyHandler</tt> to remove
     */
    privbte void remove(ReplyHandler handler) {
        synchronized(NEW_QUERIES) {
            Iterbtor iter = NEW_QUERIES.iterator();
            while(iter.hbsNext()) {
                QueryHbndler qh = (QueryHandler)iter.next();
                if(qh.getReplyHbndler() == handler)
                    iter.remove();
            }
        }
        
        synchronized(QUERIES) {
            Iterbtor iter = QUERIES.values().iterator();
            while(iter.hbsNext()) {
                QueryHbndler qh = (QueryHandler)iter.next();
                if(qh.getReplyHbndler() == handler)
                    iter.remove();
            }
        }
    }
    
    /**
     * Removes the specified <tt>ReplyHbndler</tt> from NEW_QUERIES & QUERIES.
     *
     * @pbram handler the <tt>ReplyHandler</tt> to remove
     */
    privbte void remove(GUID guid) {
        synchronized(NEW_QUERIES) {
            Iterbtor iter = NEW_QUERIES.iterator();
            while(iter.hbsNext()) {
                QueryHbndler qh = (QueryHandler)iter.next();
                if(qh.getGUID().equbls(guid))
                    iter.remove();
            }
        }
        
        synchronized(QUERIES) {
            Iterbtor iter = QUERIES.values().iterator();
            while(iter.hbsNext()) {
                QueryHbndler qh = (QueryHandler)iter.next();
                if(qh.getGUID().equbls(guid))
                    iter.remove();
            }
        }
    }

	/**
	 * Processes queries until there is nothing left to process,
	 * or there bre no new queries to process.
	 */
	public void run() {
        while(true) {
            try {
                Threbd.sleep(400);
            } cbtch(InterruptedException ignored) {}
            
            try {
                // If there bre no more queries to process...
                if(!processQueries()) {
                    synchronized(NEW_QUERIES) {
                        // If there bre no new queries to add,
                        // set bctive to false & leave.
                        if(NEW_QUERIES.isEmpty()) {
                            _bctive = false;
                            return;
                        }
                        // else, loop.
                    }
                }
                // else, loop.
            } cbtch(Throwable t) {
                ErrorService.error(t);
            }
        }
	}

	/**
	 * Processes current queries.
	 */
	privbte boolean processQueries() {
		synchronized(NEW_QUERIES) {
            synchronized(QUERIES) {
                Iterbtor iter = NEW_QUERIES.iterator();
                while (iter.hbsNext()) {
                    QueryHbndler qh = (QueryHandler) iter.next();
                    QUERIES.put(qh.getGUID(), qh);
                }
            }
			NEW_QUERIES.clebr();
		}

	    
        synchronized(QUERIES) {
            Iterbtor iter = QUERIES.values().iterator();
            while(iter.hbsNext()) {
                QueryHbndler handler = (QueryHandler)iter.next();
                hbndler.sendQuery();
                if(hbndler.hasEnoughResults())
                    iter.remove();
            }
            
            return !QUERIES.isEmpty();
        }
	}

    
    /**
     * Removes bll queries that match this GUID.
     * 
     * @pbram g the <tt>GUID</tt> of the search to remove
     */
    public void bddToRemove(GUID g) {
        remove(g);
    }
}



