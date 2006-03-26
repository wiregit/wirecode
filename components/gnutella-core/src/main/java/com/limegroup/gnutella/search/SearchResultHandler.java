
// Commented for the Learning branch

package com.limegroup.gnutella.search;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * The program's SearchResultHandler object gets query hit packets for searches for our user, and keeps the GUI and our ultrapeers searching for us up to date.
 * 
 * A search begins with a query packet, which has a message GUID.
 * The query hit packets and all the other packets involved in the search share this GUID.
 * In this way, the GUID represents the search.
 * SearchResultHandler uses GUIDs to represent searches in this way.
 * 
 * SearchResultHandler deals with searches we're performing for our user.
 * In the RouteTable for query and query hit packets, the search GUID was entered with the ForMeReplyHandler.
 * 
 * We can be a leaf or an ultrapeer.
 * If we're a leaf, we have our ultrapeers searching on our behalf, but we're receiving query hit packets in response to their efforts for us.
 * If we're an ultrapeer, our user has searched for something, and we're doing dynamic querying for ourselves.
 * 
 * handleQueryReply() gets called with a query hit packet we've just received.
 * It makes sure the sharing computer looks reachable, and gives the hits to the GUI.
 * It keeps a count of how many hits we've gotten for our search.
 * Each time we get 15 more, we tell our ultrapeers searching for us how many we have with a BEAR 12 1 Query Status Response vendor message.
 * 
 * SearchResultHandler keeps a list called GUID_COUNTS of GuidCount objects.
 * Each GuidCount object represents a search we're performing for our user.
 * A GuidCount object keeps a GUID and a count together.
 * The GUID is the GUID that identifies the search.
 * The count is the number of hits we've gotten for the search from good sharing computers that we've told the GUI about.
 */
public final class SearchResultHandler {

    /** Make a debugging log we can write lines of text to as the program runs. */
    private static final Log LOG = LogFactory.getLog(SearchResultHandler.class);

    /**
     * 30000, 30 seconds in milliseconds.
     * 
     * Used by getQueriesToReSend().
     * We're a leaf, and the user has started a search in the last 30 seconds.
     * If we connect to a new ultrapeer that supports dynamic querying, we'll send the query packet of our recent searches to it.
     */
    private static final int QUERY_EXPIRE_TIME = 30 * 1000; // 30 seconds.

    /** 15, when we get 15 more hits for a search our ultrapeers are running for us, we'll tell them about it with a BEAR 12 1 Query Status Response vendor message. */
    public static final int REPORT_INTERVAL = 15;

    /**
     * 65535, to tell our ultrapeers to stop searching for us, we'll send them a BEAR 12 1 Query Status Response vendor message that says we have 65535 hits.
     * This is 0xffff in the 2 byte payload of the message.
	 */
    public static final int MAX_RESULTS = 65535;

    /**
     * A list of our searches, and how many hits each has gotten.
     * GUID_COUNTS is a Java Vector of GuidCount objects.
     * A GuidCount object keeps a GUID that identifies a search with a count of how many hits for it we've passed up to the GUI.
     * When we get 15 more hits for a search, we'll tell our ultrapeers searchign for us our hit total with a BEAR 12 1 Query Status Response vendor message.
     * 
     * Vector objects are synchronized, so it's safe to use them with multiple threads.
     */
    private final List GUID_COUNTS = new Vector();

    /*
     * PUBLIC INTERFACE METHODS
     */

    /**
     * Pass the hits from a query hit packet we've received up to the GUI, and tell our ultrapeers searching for us how many hits we have so they know when to stop.
     * 
     * Only ForMeReplyHandler.handleQueryReply() calls this.
     * We've received a query hit packet intended for us.
	 * 
	 * @param qr The query hit packet we received
     */
    public void handleQueryReply(QueryReply qr) {

        // Pass the hits from a query hit packet we've received up to the GUI, and tell our ultrapeers searching for us how many hits we have so they know when to stop.
        handleReply(qr);
    }

    /**
     * Add a query to the list of them the SearchResultHandler keeps so that it can count how many hits we've gotten and tell our ultrapeers this number.
     * 
     * Makes a GuidCount object for the given query packet's message GUID, which identifes the search.
     * Adds it to the GUID_COUNTS list of them.
     * The GuidCount keeps the GUID with a count of the number of hits we've received.
     * Each time we get 15 more hits, we'll tell our ultrapeers search for us how many we have.
     * This is called leaf guidance because we, the leaf, are guiding our ultrapeers as they search for us.
     * 
     * Only RouterService.recordAndSendQuery() calls this.
     * 
     * Adds the Query to the list of queries kept track of.  You should do this
     * EVERY TIME you start a query so we can leaf guide it when possible.
     * 
     * @param qr The query packet that started the search.
     *           We use it's message GUID, which identifes the search and all the packets involved in this search will have.
     */
    public void addQuery(QueryRequest qr) {

        // Make a new GuidCount object from the given query packet, and add it to our list
        LOG.trace("entered SearchResultHandler.addQuery(QueryRequest)");
        GuidCount gc = new GuidCount(qr);
        GUID_COUNTS.add(gc);
    }

    /**
     * Make a BEAR 12 1 Query Status Response vendor message that says we have 65535 hits, and send it to our ultrapeers to make them stop the search.
     * Removes the search from the list the SearchResultHandler keeps.
     * 
     * Only RouterService.stopQuery() calls this.
     * 
     * @param guid The GUID of the search we cancelled.
     *             This is the message GUID of the query packet and all the other packets involved in this search, and identifies it.
     */
    public void removeQuery(GUID guid) {

        // Make a note we're here
        LOG.trace("entered SearchResultHandler.removeQuery(GUID)");

        // Find and remove the GuidCount object with the given GUID in our list
        GuidCount gc = removeQueryInternal(guid); // Returns null if not found

        // If we had a GuidCount object in our list, and the search it keeps track of doesn't have 150 hits yet
        if ((gc != null) && (!gc.isFinished())) {

            /*
             * shut off the query at the UPs - it wasn't finished so it hasn't
             * been shut off - at worst we may shut it off twice, but that is
             * a timing issue that has a small probability of happening, no big
             * deal if it does....
             */

            // Make a BEAR 12 1 Query Status Response vendor message that says we have 65535 hits and send it to our ultrapeers, telling them to stop the search
            QueryStatusResponse stat = new QueryStatusResponse(guid, MAX_RESULTS); // Make a Query Status Response message with the search GUID and 65535 hits, 0xffff
            RouterService.getConnectionManager().updateQueryStatus(stat);          // Send it to all our ultrapeers
        }
    }

    /**
     * Get a list of the query packets we've been searching with for the last 30 seconds.
     * 
     * Only ManagedConnection.handleVendorMessage() calls this.
     * We're a leaf.
     * A remote ultrapeer we just connected to sent us its Messages Supported vendor message right after the handshake.
     * getQueriesToReSend() makes a list of all the searches we started in the last 30 seconds that don't have 150 hits yet.
     * We'll send the query packets for these searches to our new ultrapeer.
     * 
     * @return A List of QueryRequest objects, the query packets of our recent searches to send to our new ultrapeer
     */
    public List getQueriesToReSend() {

        // Make a note we're here
        LOG.trace("entered SearchResultHandler.getQueriesToSend()");

        // Make a list of the query packets that we'll send to our new ultrapeer
        List reSend = null;

        // Only let one thread at a time access our GUID_COUNTS list
        synchronized (GUID_COUNTS) {

            // Get the time now
            long now = System.currentTimeMillis();

            // Loop through each GuidCount object in our GUID_COUNTS list
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount)iter.next();

                // If the search is younger than 30 seconds, and doesn't have 150 hits yet
                if (isQueryStillValid(currGC, now)) {

                    // We're going to send this query to the ultrapeer we just connected to
                    if (LOG.isDebugEnabled()) LOG.debug("adding " + currGC + " to list of queries to resend");

                    // Make the list if it doesn't already exist
                    if (reSend == null) reSend = new LinkedList();

                    // Get the query packet for this search, and add it to the list
                    reSend.add(currGC.getQueryRequest());
                }
            }
        }

        // Return the list of query packets we prepared, or an empty list if we don't have any searches for our new ultrapeer
        if (reSend == null) return Collections.EMPTY_LIST;
        else                return reSend;
    }

    /**
     * Find out how many results we've sent up to the GUI for a given search.
     * 
     * @param guid The GUID that identifies the search, and is the message GUID of the query packet and all the other packets used in the search.
     * @return     The number of hits we've received and told the GUI about.
     *             -1 if not found.
     */
    public int getNumResultsForQuery(GUID guid) {

        // Look up the GuidCount object for the given GUID
        GuidCount gc = retrieveGuidCount(guid);

        // Return the number of hits we've told the GUI about for this search
        if (gc != null) return gc.getNumResults();
        else            return -1; // Not found, we don't have a GuidCount object in our list for the given search GUID
    }

    /*
     * END OF PUBLIC INTERFACE METHODS
     * PRIVATE INTERFACE METHODS
     */

    /**
     * Pass the hits from a query hit packet we've received up to the GUI, and tell our ultrapeers searching for us how many hits we have so they know when to stop.
     * 
     * Makes sure the computer sharing the files looks reachable, and passes the hits up to the GUI.
     * If we're a leaf and have gotten 15 more hits, tells our ultrapeers searching for us our hit count in a BEAR 12 1 Query Status Response vendor message.
     * 
     * Only ForMeReplyHandler.handleQueryReply() calls this.
     * We've received a query hit packet intended for us.
     * 
     * @param qr The query hit packet we received
     * @return   True if we told the GUI about the hits in the packet, false if we didn't
     */
    private boolean handleReply(final QueryReply qr) {

        // Make a HostData object that holds the information about the computer that made the query packet and is sharing the files it describes
        HostData data;
        try {
            data = qr.getHostData();
        } catch(BadPacketException bpe) {
            LOG.debug("bad packet reading qr", bpe);
            return false;
        }

        /*
         * always handle reply to multicast queries
         */

        // If this is a regular Internet query hit, make sure we'll be able to connect to the remote computer
        if (!data.isReplyToMulticastQuery() && // The query packet doesn't have GGEP "MCAST", it's not a reply to a multicast query, and
            !qr.isBrowseHostReply()) {         // We haven't marked the query packet as a response to a browse host request

            /*
             * note that the minimum search quality will always be greater
             * than -1, so -1 qualities (the impossible case) are never
             * displayed
             */

            // If the information we have about the sharing computer looks bad, don't tell the GUI about the results
            if (data.getQuality() < SearchSettings.MINIMUM_SEARCH_QUALITY.getValue()) {

                // Leave without doing anything
                LOG.debug("Ignoring because low quality");
                return false;
            }

            // The sharing computer told us its upload speed in the query hit packet, if that's too slow for settings, don't tell the GUI about the results
            if (data.getSpeed() < SearchSettings.MINIMUM_SEARCH_SPEED.getValue()) {

                // Leave without doing anything
                LOG.debug("Ignoring because low speed");
                return false;
            }

            /*
             * if the other side is firewalled AND
             * we're not on close IPs AND
             * (we are firewalled OR we are a private IP) AND
             * no chance for FW transfer then drop the reply.
             */

            // If we can't connect to the sharing computer, don't tell the GUI about the results
            if (

                // If we can't connect to the sharing computer because it's firewalled, and
                data.isFirewalled() &&

                // The sharing computer's IP address is very similar to our own, and
                !NetworkUtils.isVeryCloseIP(qr.getIPBytes()) &&

                // We're firewalled
                (!RouterService.acceptedIncomingConnection() ||               // Our TCP connect back requests have failed, or
                NetworkUtils.isPrivateAddress(RouterService.getAddress())) && // The IP address we tell people is just our internal LAN address

                // We can't do firewall-to-firewall file transfers, or the sharing computer can't
                !(UDPService.instance().canDoFWT() && qr.getSupportsFWTransfer())) {

                // Leave without doing anything
                LOG.debug("Ignoring from firewall funkiness");
                return false;
            }
        }

        // Get the file information blocks from the query hit packet
        List results = null;
        try {
            results = qr.getResultsAsList(); // Get a List of Response objects
        } catch (BadPacketException e) {
            LOG.debug("Error gettig results", e);
            return false;
        }

        // Loop through the hits from the packet
        int numSentToFrontEnd = 0; // Count how many hits we send up to the GUI
        for (Iterator iter = results.iterator(); iter.hasNext(); ) {
            Response response = (Response)iter.next();

            // These hits aren't from the result of a brose host request
            if (!qr.isBrowseHostReply()) {

                // If the type or query (do) of the hit doesn't match our search, skip it
            	if (!RouterService.matchesType(data.getMessageGUID(), response)) continue;
            	if (!RouterService.matchesQuery(data.getMessageGUID(), response)) continue;
            }

            // If this is a result from the Mandragore worm, skip it
        	if (RouterService.isMandragoreWorm(data.getMessageGUID(), response)) continue;

        	// Make a RemoteFileDesc object from the Response object
            RemoteFileDesc rfd = response.toRemoteFileDesc(data);

            // From the GGEP "ALT" extension, get a HashSet of Endpoint objects with the IP addresses of other computers that have this file
            Set alts = response.getLocations();

            // Pass the RemoteFileDesc, query packet, and alternate locations list up to the GUI
            RouterService.getCallback().handleQueryResult(rfd, data, alts);

            // Count that we passed another hit up to the GUI
            numSentToFrontEnd++;
        }

        /*
         * ok - some responses may have got through to the GUI, we should account
         * for them....
         */

        // If we've gotten 15 more hits, send a BEAR 12 1 Query Status Response vendor message to our ultrapeers
        accountAndUpdateDynamicQueriers(qr, numSentToFrontEnd);

        // Return true if we gave some hits to the GUI, false if we filtered them all out
        return (numSentToFrontEnd > 0);
    }

    /**
     * If we've gotten 15 more hits, send a BEAR 12 1 Query Status Response vendor message to our ultrapeers.
     * 
     * If we're a leaf, our ultrapeers are searching for us as a part of LimeWire's dynaimc querying system.
     * Our job is to tell them how many hits we have gotten that we've liked, so they know to keep searching or to stop.
     * This method does this every time we get 15 more hits.
     * It makes a BEAR 12 1 Query Status Response vendor message with the search GUID and our hit count divided by 4.
     * We divide it by 4 because we're probably connected up to about 4 ultrapeers.
     * We tell the ultrapeers the number of search results we'd like them each to get for us.
     * 
     * @param qr                A query packet we received with hits in response to a search we made
     * @param numSentToFrontEnd The number of hits we told the GUI about
     */
    private void accountAndUpdateDynamicQueriers(final QueryReply qr, final int numSentToFrontEnd) {

        // Make a note we're here
        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): entered.");

        /*
         * we should execute if results were consumed
         * technically Ultrapeers don't use this info, but we are keeping it
         * around for further use
         */

        // We told the GUI about some hits in the packet
        if (numSentToFrontEnd > 0) {

            // Get the GuidCount object we made for this GUID and put in the GUID_COUNTS list
            GuidCount gc = retrieveGuidCount(new GUID(qr.getGUID()));
            if (gc == null) // Not found

                /*
                 * 0. probably just hit lag, or....
                 * 1. we could be under attack - hits not meant for us
                 * 2. programmer error - ejected a query we should not have
                 */

                // Leave now
                return;

            // Add the number of hits we told the GUI about to the number in the GuidCount object that represents this search 
            LOG.trace("SRH.accountAndUpdateDynamicQueriers(): incrementing.");
            gc.increment(numSentToFrontEnd);

            // If we're a leaf
            if (RouterService.isShieldedLeaf()) {

                // If we haven't gotten 150 hits yet, but we've gotten 15 more than the last time
                if (!gc.isFinished() &&                             // The code in this if block hasn't marked this GuidCount as finished yet, and
                    (gc.getNumResults() > gc.getNextReportNum())) { // We've gotten 15 more results since we last told our ultrapeer about them

                    // Make a note that we're going to tell our ultrapeers how many hits we've gotten
                    LOG.trace("SRH.accountAndUpdateDynamicQueriers(): telling UPs.");

                    // Set getNextReportNum() to 15 more results than the number we have now
                    gc.tallyReport();

                    // If we have told the GUI more than 150 results, we're done with this search
                    if (gc.getNumResults() > QueryHandler.ULTRAPEER_RESULTS) gc.markAsFinished();

                    /*
                     * if you think you are done, then undeniably shut off the
                     * query.
                     */

                    // Calculate the number of hits we have divided by 4, the number of ultrapeers we have
                    final int numResultsToReport =
                        (gc.isFinished() ? // If we got more than 150 results and are done
                        MAX_RESULTS :      // Just use 150 for this calculation
                        gc.getNumResults() // Otherwise, use the number less than 150 that we actually have
                        / 4);              // Divide that by 4, assuming we have 4 ultrapeers

                    // Make a new BEAR 12 1 Query Status Response vendor message with the search GUID and the hit count we split into 4
                    QueryStatusResponse stat = new QueryStatusResponse(
                        gc.getGUID(),        // The GUID that identifes the search
                        numResultsToReport); // The number of results we'd like each of our 4 ultrapeers to give us

                    // Send the packet to all our ultrapeers
                    RouterService.getConnectionManager().updateQueryStatus(stat);
                }
            }
        }

        // Make a note we're done
        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): returning.");
    }

    /**
     * Remove and return the GuidCount object with a given GUID from our GUID_COUNTS list of them.
     * 
     * @param guid The message GUID of a query packet that began a search, and represents it.
     * @return     The GuidCount object with that GUID that was in our list and that we removed.
     *             null if not found.
     */
    private GuidCount removeQueryInternal(GUID guid) {

        // Only let one thread access the GUID_COUNTS list at a time
        synchronized (GUID_COUNTS) {

            // Loop for each GuidCount object in our list
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount)iter.next();

                // We found the one for the given GUID
                if (currGC.getGUID().equals(guid)) {

                    // Remove it and return it
                    iter.remove();
                    return currGC;
                }
            }
        }

        // Not found
        return null;
    }

    /**
     * Look up a GuidCount object in our GUID_COUNTS list of them.
     * 
     * @param guid The GUID of the one we're looking for
     * @return     The GuidCount object in the list with that GUID
     */
    private GuidCount retrieveGuidCount(GUID guid) {

        // Only let one thread access the GUID_COUNTS list of GuidCount objects at a time
        synchronized (GUID_COUNTS) {

            // Loop for each GuidCount object in our GUID_COUNTS list
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount)iter.next();

                // If we found it, return it
                if (currGC.getGUID().equals(guid)) return currGC;
            }
        }

        // Not found
        return null;
    }

    /**
     * Determine if the given search is still younger than 30 seconds, and doesn't have 150 hits yet.
     * 
     * @param gc  A GuidCount object that represents a search
     * @param now The time now
     * @return    True if we started the search less than 30 seconds ago, and we still don't have 150 hits for it yet
     */
    private boolean isQueryStillValid(GuidCount gc, long now) {

        // Make a note we're here
        LOG.trace("entered SearchResultHandler.isQueryStillValid(GuidCount)");

        // Return true if we started the given search less than 30 seconds ago, and don't have 150 hits for it yet
        return
            (now < (gc.getTime() + QUERY_EXPIRE_TIME)) &&          // This search isn't 30 seconds old yet, and
            (gc.getNumResults() < QueryHandler.ULTRAPEER_RESULTS); // We still don't have 150 hits for it yet
    }

    /*
     * END OF PRIVATE INTERFACE METHODS
     */

    /**
     * A GuidCount keeps a GUID and a hit count together.
     * The GUID identifes a search, it's the message GUID of the query packet and all the other packets that are a part of the search.
     * The hit count is the number of hits for the search that we've checked out, and told the GUI about.
     */
    private static class GuidCount {

        /** The time this GuidCount object was made. */
        private final long _time;

        /** The GUID that identifies the search, and is the message GUID of the query packet and all the other packets. */
        private final GUID _guid;

        /** The query packet. */
        private final QueryRequest _qr;

        /** The number of hits we've gotten for the search. */
        private int _numResults;

        /** When we have this many results, we'll tell our ultrapeer. */
        private int _nextReportNum = REPORT_INTERVAL; // Start it at 15

        /** True to tell the GUI to mark this search as finished. */
        private boolean markAsFinished = false;

        /**
         * Make a new GuidCount object that will keep a query packet and a number of hits.
         * 
         * @param qr The query packet
         */
        public GuidCount(QueryRequest qr) {

            // Save the query packet
            _qr = qr;

            // Pull out the GUID sepretly and keep a copy of it
            _guid = new GUID(qr.getGUID());

            // Start the hit count at 0
            _numResults = 0;

            // Record that we made this object now
            _time = System.currentTimeMillis();
        }

        /**
         * Get the GUID that identifies the search, and is the message GUID of the query packet and all the other packets.
         * 
         * @return The GUID
         */
        public GUID getGUID() {

            // Return the GUID the constructor pulled from the query packet
            return _guid;
        }

        /**
         * Get the number of hits this GuidCount object has counted for the search.
         * 
         * @return The hit count number
         */
        public int getNumResults() {

            // Return the result number which started at 0, and increment(incr) has been increasing
            return _numResults;
        }

        /**
         * When we have this many results, we'll tell our ultrapeer.
         * 
         * @return The number of results
         */
        public int getNextReportNum() {

            // Return the number 15
            return _nextReportNum;
        }

        /**
         * Get the time we made this GuidCount object.
         * 
         * @return The time, the number of milliseconds since 1970
         */
        public long getTime() {

            // Return the time
            return _time;
        }

        /**
         * Get the query packet this GuidCount object keeps.
         * 
         * @return The QueryRequest object that represents the query packet
         */
        public QueryRequest getQueryRequest() {

            // Return the query packet
            return _qr;
        }

        /**
         * Determine if this GuidCount object marks its search as finished or not.
         * The markAsFinished() method sets this.
         * 
         * @return True if we marked this GuidCount search as finished
         */
        public boolean isFinished() {

            // Return the flag value
            return markAsFinished;
        }

        /**
         * Set _nextReportNum to the number of total hits we'll have to have to tell the GUI about them.
         * Sets _nextReportNum = _numResults + 15.
         */
        public void tallyReport() {

            // Set _nextReportNum to 15 more hits that we have right now
            _nextReportNum = _numResults + REPORT_INTERVAL;
        }

        /**
         * Have this GuidCount object count the given number of additional hits.
         * 
         * @param incr The number of additional hits we've received for this search
         */
        public void increment(int incr) {

            // Add the given number to our total
            _numResults += incr;
        }

        /**
         * Mark this GuidCount as finished.
         * After you call this, isFinished() will start returning true.
         */
        public void markAsFinished() {

            // Set the finished flag to true
            markAsFinished = true;
        }

        /**
         * Express this GuidCount object as text.
         * 
         * @return A String
         */
        public String toString() {

            // Compose text with the search GUID, hit total, and number of hits we have to reach before telling the GUI about them
            return "" + _guid + ":" + _numResults + ":" + _nextReportNum;
        }
    }
}
