
// Commented for the Learning branch

package com.limegroup.gnutella.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * The program's QueryDispatcher object keeps a list of QueryHandler searches, and has the "QueryDispatcher" thread call sendQuery() on them every 0.4 seconds.
 * 
 * The program makes one QueryDispatcher object.
 * It keeps a list called QUERIES of QueryHandler objects.
 * Give it one with addQuery(QueryHandler).
 * The QueryDispatcher has a ProcessingQueue that makes a thread named "QueryDispatcher".
 * It loops every 0.4 seconds.
 * For each loop, it calls calls queryHandler.sendQuery() and then queryHandler.hasEnoughResults() on each QueryHandler object in the list.
 * queryHandler.sendQuery() sends out the next query packet if its time to do so.
 * queryHandler.hasEnoughResults() tells if we should stop or not.
 * If a QueryHandler has enough results, the search is done, and we remove it from the list.
 * 
 * Manages dynamic querying for Ultrapeers.
 * 
 * This maintains the data for all active queries for this Ultrapeer and any
 * of its leaves, also providing an interface for removing active queries.
 * Queries may be removed, for example, when a leaf node with an active query
 * disconnects from the Ultrapeer.
 */
public final class QueryDispatcher implements Runnable {

	/**
     * A list of the searches the program is performing.
     * 
     * QUERIES is a Java HashMap that has GUID keys that map to QueryHandler values.
     * 
     * A key is a GUID that represents a search.
     * It's the message GUID of the query packet that started the search.
     * All the packets involved in the search have this as their message GUID.
     * 
     * The value is a QueryHandler object.
     * It keeps the QueryRequest, ManagedConnection, and RouteTableEntry objects related to the search together.
     * It also keeps a count of how many hits we've gotten, or the leaf we're searching for has.
	 */
	private final Map QUERIES = new HashMap();

	/**
	 * The list of new queries we need to add to the QUERIES list.
	 * 
	 * The addQuery(QueryHandler) method adds a given QueryHandler to the NEW_QUERIES list.
	 * The "QueryDispatcher" thread moves it from that list to this one.
     * 
     * NEW_QUERIES is a synchronized LinkedList of QueryHandler objects.
     * 
	 * LOCKING: Thread-safe, although you must obtain a lock on NEW_QUERIES if
	 * it's ever iterated over.
	 */
	private final List NEW_QUERIES = Collections.synchronizedList(new LinkedList());

	/**
     * The program's single QueryDispatcher object.
     * Java runs this line of code when it loads the class.
     * It creates the object.
     */
	private static final QueryDispatcher INSTANCE = new QueryDispatcher();

    /**
     * The QueryDispatcher has a ProcessingQueue that makes a thread named "QueryDispatcher" and calls the run() method.
     * A ProcessingQueue takes objects with run() methods, calls them one by one, and then exits.
     * We only add one object to our ProcessingQueue, this QueryDispatcher object.
     */
    private final ProcessingQueue PROCESSOR = new ProcessingQueue("QueryDispatcher");

    /** True if the ProcessingQueue has this QueryDispatcher object listed in it, and has its "QueryDispatcher" thread executing the code in the run() method. */
    private boolean _active;

	/**
     * Get the program's single QueryDispatcher object.
     * MessageRouter.DYNAMIC_QUERIER calls this to access it that way.
     * 
     * @return The QueryDispatcher
	 */
	public static QueryDispatcher instance() {

        // Return the object the static code made
		return INSTANCE;
	}

	/** Don't let outside code make a QueryDispatcher object. */
	private QueryDispatcher() {}

	/**
     * Add the given QueryHandler to our list of searches to run.
     * 
     * Calls handler.sendQuery() right away to have it send the first query packet.
     * Adds handler to this QueryDispatcher ProcessingQueue to have the QueryDispatcher thread call sendQuery() later.
	 * 
	 * @param handler The QueryHandler object that represents the search and contains its query packet
	 */
	public void addQuery(QueryHandler handler) {

        // Immediately send out one query
        handler.sendQuery(); // We'll call sendQuery() repeatedly to perform all the steps of the search

        // Only let one thread use the NEW_QUERIES list at a time
        synchronized (NEW_QUERIES) {

            // Add the given object to the list
		    NEW_QUERIES.add(handler);

            // Start the QueryDispatcher ProcessingQueue thread if its not running right now
		    if (NEW_QUERIES.size() == 1 && // We didn't have any searches before the one we're just running
                !_active) {                // The QueryDispatcher isn't active right now

                // Mark the QueryDispatcher as active, and add the QueryDispatcher object to the ProcessingQueue that has a thread call its run() method
		        _active = true;
		        PROCESSOR.add(this);
            }
		}
	}

    /**
     * Remove all the searches we're performing for a remote computer we're disconnecting from.
     * MessageRouter.removeConnection() calls this when we're disconnecting from a remote computer.
     * 
     * @param handler The ManagedConnection object we just closed
     */
    public void removeReplyHandler(ReplyHandler handler) {

        // If it's not a connection down to one of our leaves, we don't have to do anything
        if (!handler.isSupernodeClientConnection()) return; // We only proxy searches for our leaves

        // Remove the searches in our lists that reference the given ManagedConnection object
        remove(handler);
    }

    /**
     * When a leaf we're searching for tells us how many hits it has, save the number in our object for the search.
     * 
     * Only MessageRouter.handleQueryStatus() calls this.
     * We're an ultrapeer, running a search for one of our leaves.
     * It sent us a BEAR 12 1 Query Status Response vendor message.
     * The message contains 2 important pieces of information:
     * Its message GUID identifies the search we're performing for this leaf.
     * All the packets related to this search have this message GUID.
     * The number in its payload tells us how many results the leaf has.
     * 
     * Finds the QueryHandler object that represents our search in our list of searches we're doing.
     * Keeps the new number from the leaf in its _numResultsReportedByLeaf member variable.
     * 
     * @param queryGUID  The message GUID of the BEAR 12 1 QueryStatusResponse, which identifies the search and is used by the query packet and all the other packets related to it
     * @param numResults The number of results the leaf says it has
     */
    public void updateLeafResultsForQuery(GUID queryGUID, int numResults) {

        // Only let one thread access the QUERIES map at a time
        synchronized (QUERIES) {

            // Look up the GUID in our QUERIES map to find the QueryHandler object that represents our search
            QueryHandler qh = (QueryHandler)QUERIES.get(queryGUID);
            if (qh != null) {

                // If the number is bigger, save it in qh._numResultsReportedByLeaf
                qh.updateLeafResults(numResults);
            }
        }
    }

    /**
     * We're performing this search for a leaf, find out how many results it's told us it has.
     * Returns the most recent count the leaf has told us with a BEAR 12 1 QueryStatusResponse message.
     * 
     * Gets the number of results the Leaf has reported so far.
     * @return a non-negative number if the guid exists, else -1.
     * 
     * @return The number of hits the leaf says it has.
     *         -1 if our QUERIES list doesn't have a QueryHandler object for this search.
     */
    public int getLeafResultsForQuery(GUID queryGUID) {

        // Only let one thread access the QUERIES list at once
        synchronized (QUERIES) {

            // Look up the search by its GUID in the QUERIES list
            QueryHandler qh = (QueryHandler)QUERIES.get(queryGUID);
            if (qh == null) return -1;                               // Not found
            else            return qh.getNumResultsReportedByLeaf(); // Get the number we saved in the QueryHandler object
        }
    }

    /**
     * Remove all the QueryHandler objects that reference the given ManagedConnection object from our NEW_QUERIES and QUERIES lists.
     * These are searches we've been running on behalf of a leaf of ours that we've disconnected from.
     * 
     * @param handler The ManagedConnection object that represents one of our leaves we've just disconnected from
     */
    private void remove(ReplyHandler handler) {

        // Loop for each QueryHandler in the NEW_QUERIES list
        synchronized (NEW_QUERIES) {
            Iterator iter = NEW_QUERIES.iterator();
            while (iter.hasNext()) {
                QueryHandler qh = (QueryHandler)iter.next();

                // If this QueryHandler would forward reply packets to the disconnected leaf, remove it from the list
                if (qh.getReplyHandler() == handler) iter.remove();
            }
        }

        // Loop for each QueryHandler in the QUERIES map
        synchronized (QUERIES) {
            Iterator iter = QUERIES.values().iterator();
            while (iter.hasNext()) {
                QueryHandler qh = (QueryHandler)iter.next();

                // If this QueryHandler would forward reply packets to the disconnected leaf, remove it from the list
                if (qh.getReplyHandler() == handler) iter.remove();
            }
        }
    }

    /**
     * Remove all the QueryHandler objects that have the given GUID from the lists the QueryDispatcher keeps.
     * This cancels the search.
     * RouterService.stopQuery(GUID) calls addToRemove(GUID) which calls here.
     * 
     * @param guid The GUID that identifies the search.
     *             This is the message GUID of the first query packet, and all the other packets involved in the search.
     */
    private void remove(GUID guid) {

        // Loop for each QueryHandler in the NEW_QUERIES list
        synchronized (NEW_QUERIES) {
            Iterator iter = NEW_QUERIES.iterator();
            while (iter.hasNext()) {
                QueryHandler qh = (QueryHandler)iter.next();

                // If this is it, remove it
                if (qh.getGUID().equals(guid)) iter.remove();
            }
        }

        // Loop for each QueryHandler in the QUERIES map
        synchronized (QUERIES) {
            Iterator iter = QUERIES.values().iterator();
            while (iter.hasNext()) {
                QueryHandler qh = (QueryHandler)iter.next();

                // If this is it, remove it
                if (qh.getGUID().equals(guid)) iter.remove();
            }
        }
    }

	/**
     * Call sendQuery() on the QueryHandler objects in our list until they all have enough hits.
     * 
     * The PROCESSOR ProcessingQueue has its "QueryDispatcher" thread call this run method.
     * Every 0.4 seconds, calls sendQuery() on each QueryHandler in our list.
     * Removes those that have enough hits and are done.
     * Exits when we don't have any more.
	 */
    public void run() {

        // Loop until the NEW_QUERIES list doesn't have any more QueryHandler objects because they all said handler.hasEnoughResults()
        while (true) {

            // Pause here for 0.4 seconds
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}

            try {

                // Call sendQuery() on each QueryHandler in our list, removing those that say they have enough hits.
                if (!processQueries()) { // If we finished them all

                    /*
                     * If there are no new queries to add,
                     * set active to false & leave.
                     */

                    // And addQuery() hasn't given us any new ones
                    synchronized (NEW_QUERIES) {
                        if (NEW_QUERIES.isEmpty()) {

                            // Mark the QueryDispatcher inactive, and return to close the "QueryDispatcher" thread
                            _active = false;
                            return;
                        }

                        /*
                         * Otherwise, loop to have processQueries() get the new search and do it
                         */
                    }
                }

                /*
                 * Keep looping to, after waiting 0.4 seconds, call sendQuery() on all our QueryHandler objects to keep the searches they represent going
                 */

            // Give exceptions to the ErrorService
            } catch (Throwable t) { ErrorService.error(t); }
        }
    }

	/**
     * Call sendQuery() on each QueryHandler in our list, and remove those that say they have enough hits and are done.
     * 
     * Moves all the QueryHandler objects addQuery() added to NEW_QUERIES from that list to the QUERIES map.
     * Loops for each QueryHandler there, calling sendQuery() on it, and then removing it from the list if it has enough results.
     * Only run() above calls this method.
     * 
     * @return True if the QUERIES map still has some QueryHandler objects we need to call sendQuery() on a little later.
     *         False if all the QueryHandler objects in QUERIES got enough hits, and we removed them to make the map empty.
	 */
	private boolean processQueries() {

        // Move all the QueryHandler objects addQuery() added to NEW_QUERIES from that list to the QUERIES map
		synchronized (NEW_QUERIES) { // Make the "QueryDispatcher" thread wait here until other threads leave the NEW_QUERIES and QUERIES lists
            synchronized (QUERIES) {

                // Loop for each QueryHandler object that addQuery() added to the NEW_QUERIES list
                Iterator iter = NEW_QUERIES.iterator();
                while (iter.hasNext()) {
                    QueryHandler qh = (QueryHandler)iter.next();

                    // List it under its GUID in the QUERIES map
                    QUERIES.put(qh.getGUID(), qh);
                }
            }

            // Now that we've copied all the QueryHandler objects from NEW_QUERIES to QUERIES, clear NEW_QUERIES to make it a move
			NEW_QUERIES.clear();
		}

        // With everything moved over, we can just synchronize on the QUERIES map
        synchronized (QUERIES) {

            // Loop for each QueryHandler object there
            Iterator iter = QUERIES.values().iterator();
            while (iter.hasNext()) {
                QueryHandler handler = (QueryHandler)iter.next();

                // Call sendQuery() on the QueryHandler to have it send another query packet if it's time to do that
                handler.sendQuery();

                // If the handler says the search is done, remove it from the list
                if (handler.hasEnoughResults()) iter.remove();
            }

            // Return true if we still have some QueryHandler objects in the QUERIES list
            return !QUERIES.isEmpty();
        }
	}

    /**
     * Cancel a search.
     * RouterService.stopQuery(GUID) calls this to cancel a search.
     * Removes the QueryHandler object stored under the given message GUID from the NEW_QUERIES and QUERIES lists.
     * 
     * @param g The GUID that identifies the search.
     *          This is the message GUID of the query packet, and all the other packets in the search.
     */
    public void addToRemove(GUID g) {

        // Remove the QueryHandler object stored under the given message GUID from the NEW_QUERIES and QUERIES lists
        remove(g);
    }
}
