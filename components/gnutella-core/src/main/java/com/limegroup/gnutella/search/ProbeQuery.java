
// Commented for the Learning branch

package com.limegroup.gnutella.search;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * A ProbeQuery is the first step in our dynamic query process, sending TTL 1 and 2 query packets to our fellow connected ultrapeers.
 * 
 * LimeWire only searches when it's an ultrapeer, so this code and the QueryHandler code only runs when we're an ultrapeer.
 * A QueryHandler represents a search, which the program performs in many steps separated by time intervals.
 * The first step is to send out a probe query.
 * A probe query consists of sending TTL 1 and TTL 2 copies of our query packet to our connected fellow ultrapeers.
 * 
 * Ultrapeers get query routing tables from their leaves.
 * They combine them into a single composite query routing table, and send that to their ultrapeers.
 * ProbeQuery uses these composite query routing tables to determine how popular what we're searching for is.
 * We can determine how popular a search is without making any network communications.
 * We just see how many query route tables block our search.
 * 
 * This class is only used in the method QueryHandler.sendQuery().
 * It calls 3 methods, like this:
 * 
 * ProbeQuery pq = new ProbeQuery(_connectionManager.getInitializedConnections(), queryHandler);
 * long timeToWait = pq.getTimeToWait();
 * _theoreticalHostsQueried += pq.sendProbe();
 * 
 * The ProbeQuery constructor takes a list of our connected fellow ultrapeers, and the QueryHandler object making the new ProbeQuery.
 * Code here sorts the ultrapeers into 2 lists: one to send TTL 1 query packets to, the other to send TTL 2 packets.
 * Next, sendQuery() asks the ProbeQuery object how long it should wait for responses to come back.
 * Finally, sendQuery() calls ps.sendProbe() to have the ProbeQuery send the TTL 1 and TTL 2 query packets.
 * 
 * This class handles query "probes."  Probe queries are the initial queries
 * that are sent out to determine the popularity of the file.  This allows
 * queries down new connections to have more information for choosing the TTL.
 */
final class ProbeQuery {

    /** To send this probe query, set the TTL of the query packet to 1 and send it to these connected fellow ultrapeers. */
    private final List TTL_1_PROBES;

    /** To send this probe query, set the TTL of the query packet to 2 and send it to these connected fellow ultrapeers. */
    private final List TTL_2_PROBES;

    /** The program's QueryHandler object that keeps a list of our searches. */
    private final QueryHandler QUERY_HANDLER;

    /**
     * Make a new ProbeQuery object, which lists the ultrapeers we should search with TTL 1 and 2 query packets.
     * 
     * Only QueryHandler.sendQuery() calls this.
     * We haven't sent the probe query yet.
     * It calls this constructor to make a new ProbeQuery, find out how long we should wait for results with getTimeToWait(), and then calls sendProbe().
     * 
     * @param connections A list of the fellow ultrapeers we're connected to
     * @param qh          The QueryHandler object that represents the search
     */
    ProbeQuery(List connections, QueryHandler qh) {

        // Save the QueryHandler object
        QUERY_HANDLER = qh;

        // Sort our ultrapeers into 2 lists, one to search with a TTL 1 query packet, the other to search with a TTL 2 query packet
        LinkedList[] lists = createProbeLists(connections, qh.QUERY);
        TTL_1_PROBES = lists[0];
        TTL_2_PROBES = lists[1];
    }

    /**
     * Find out how long the program should wait for hits to get back to us after we send out the query packets of this probe query.
     * 
     * @return The time to wait, in milliseconds
     */
    long getTimeToWait() {

        /*
         * determine the wait time.  we wait a little longer per
         * hop for probes to give them more time -- also weight
         * this depending on how many TTL = 1 probes we're sending
         */

        // If this ProbeQuery will send TTL 2 query packets to some ultrapeers, return 2400 * 1.3 = 3.12 seconds
        if (!TTL_2_PROBES.isEmpty()) return (long)((double)QUERY_HANDLER.getTimeToWaitPerHop() * 1.3);

        // This ProbeQuery won't send any TTL 2 query packets, if it will send some TTL 1 query packets, return 1.2 seconds * the number of ultrapeers we'll query
        if (!TTL_1_PROBES.isEmpty()) return (long)((double)QUERY_HANDLER.getTimeToWaitPerHop() * ((double)TTL_1_PROBES.size() / 2.0));

        // This ProbeQuery won't send any packets at all, wait no time
        return 0L;
    }

    /**
     * Send the query packets of this ProbeQuery to the ultrapeers in the lists we composed.
     * 
     * @return The number of computers we estimate our search reached
     */
    int sendProbe() {

    	//zootella
    	System.out.println("Sending probe query with TTL 1 to " + TTL_1_PROBES.size() + " ultrapeers, and TTL 2 to " + TTL_2_PROBES.size() + " ultrapeers");

        // Make iter to loop through the ultrapeers we'll send our query packet with a TTL of 1 to
        Iterator iter = TTL_1_PROBES.iterator();

        // Count the number of computers we estimate our search has reached in host
        int hosts = 0;

        // Copy the query packet we'll use in this search, setting its TTL to 1
        QueryRequest query = QUERY_HANDLER.createQuery((byte)1);

        // Loop for each ultrapeer in our TTL 1 list
        while (iter.hasNext()) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            // Send the TTL 1 query packet to the remote computer
            hosts += QueryHandler.sendQueryToHost(query, mc, QUERY_HANDLER); // Returns the number of computers we estimate that reached
        }

        // Copy the query packet we'll use in this search, setting its TTL to 2
        query = QUERY_HANDLER.createQuery((byte)2);

        // Loop for each ultrapeer in our TTL 2 list
        iter = TTL_2_PROBES.iterator();
        while (iter.hasNext()) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            // Send the TTL 2 query packet to the remote computer
            hosts += QueryHandler.sendQueryToHost(query, mc, QUERY_HANDLER); // Returns the number of computers we estimate that reached
        }

        // Clear our lists of ultrapeers, we don't need them anymore
        TTL_1_PROBES.clear();
        TTL_2_PROBES.clear();

        // Return the number of hosts our search reached
        return hosts;
    }

    /**
     * Compose lists of ultrapeers for us to search in the probe query.
     * 
     * Determines how popular what we're searching for is by seeing how many ultrapeer query routing tables block the search.
     * If the search makes it through every query route table, it's very popular.
     * createProbeLists() puts 1 ultrapeer in the TTL 1 list, and returns it.
     * 
     * If the search makes it through less than 3 of our ultrapeers, it's rare.
     * Composes lists to send it with a TTL of 1 to all our ultrapeers with accepting query route tables.
     * Additionally, sent it with a TTL of 2 to up to 3 ultrapeers that don't have query route tables, or have query route tables that block our search.
     * 
     * @param connections A list of the fellow ultrapeers we're connected to.
     * @param query       The query packet that we'll be searching with.
     * @return            An array of 2 LinkedList objects.
     *                    Give the query packet a TTL of 1 to send it to the ultrapeers in the [0] list.
     *                    Give the query packet a TTL of 2 to send it to the ultrapeers in the [1] list.
     */
    private static LinkedList[] createProbeLists(List connections, QueryRequest query) {

        // Get an Iterator to loop through the fellow ultrapeers we're connected to
        Iterator iter = connections.iterator();

        // We'll sort our connected ultrapeers into one of these 3 lists
        LinkedList missConnections = new LinkedList(); // Ultrapeers with query route tables that block our search
        LinkedList oldConnections  = new LinkedList(); // Ultrapeers that don't have a query route table
        LinkedList hitConnections  = new LinkedList(); // Ultrapeers that have query route tables our search makes it through

        /*
         * iterate through our connections, adding them to the hit, miss, or
         * old connections list
         */

        // Loop for each fellow ultrapeer we're connected to
        while (iter.hasNext()) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            // This remote ultrapeer said "X-Ultrapeer-Query-Routing: 0.1" or higher, meaning it can exchange a query route table with us
            if (mc.isUltrapeerQueryRoutingConnection()) {

                // See if our search will pass through or hit the remote ultrapeers query route table
                if (mc.shouldForwardQuery(query)) hitConnections.add(mc);  // Our search makes it through this ultrapeer's query route table
                else                              missConnections.add(mc); // This ultrapeer's query route table blocks our search

            // The remote ultrapeer can't give us a query route table
            } else {

                // Add it to our list of old connections
                oldConnections.add(mc);
            }
        }

        /*
         * final list of connections to query
         */

        // Make the structure of lists we'll return
        LinkedList[] returnLists = new LinkedList[2]; // Make the array of 2 LinkedList objects we'll return
        LinkedList ttl1List = new LinkedList();       // Make new LinkedList objects to put in the array
        LinkedList ttl2List = new LinkedList();
        returnLists[0] = ttl1List;                    // Put them in the array
        returnLists[1] = ttl2List;

        /*
         * do we have adequate data to determine some measure of the file's
         * popularity?
         */

        // If we're connected to at least 8 ultrapeers that have given us query route tables, that's enough to determine how popular it is
        boolean adequateData = (missConnections.size() + hitConnections.size()) > 8; // Set adequateData to false if we don't have enough

        /*
         * if we don't have enough data from QRP tables, just send out a
         * traditional probe also, if we don't have an adequate number of QRP
         * tables to access the popularity of the file, just send out an
         * old-style probe at TTL = 2
         */

        // If our search won't make it through the query route tables of any of our ultrapeers, or we don't have query route tables from 8
        if (hitConnections.size() == 0 || !adequateData) {

            // Compose the lists of ultrapeers for us to query in an agressive probe, and return it
            return createAggressiveProbe(
                oldConnections,  // Ultrapeers that don't have a query route table
                missConnections, // Ultrapeers with query route tables that block our search
                hitConnections,  // Ultrapeers with query route tables our search makes it through
                returnLists);    // An array of 2 LinkedList objects
                                 // returnLists[0] contains up to 4 ultrapeers from the hitConnections list, our search passes through their query route tables
                                 // returnLists[1] contains up to 3 ultrapeers from the oldConnections and missConnections lists
        }

        // Determine how popular our search is by finding the fraction of ultrapeer query route tables it passes through
        int numHitConnections = hitConnections.size();
        double popularity = (double)
            ((double)numHitConnections /                           // The number of ultrapeers with query route tables our search passes through, divided by
            ((double)missConnections.size() + numHitConnections)); // The total number of connections with query route tables

        /*
         * if the file appears to be very popular, send it to only one host
         */

        // If our search makes it through every single query route table
        if (popularity == 1.0) { // Our search makes it through every single query route table, missConnections.size() is 0

            // Compose lists to send our probe query with a TTL of 1 to just 1 ultrapeer
            ttl1List.add(hitConnections.removeFirst());
            return returnLists; // Return the one ultrapeer in our list, leave the second list empty
        }

        // Our search is popular enough to make it through the query route tables of more than 3 of our ultrapeers
        if (numHitConnections > 3) {

            /*
             * TTL = 1 queries are cheap -- send a lot of them if we can
             */

            // Compose lists to send our probe query with a TTL of 1 to up to 9 ultrapeers
            int numToTry = Math.min(9, numHitConnections); // We'll try up to 9 ultrapeers that have query route tables our search passes through
            int startIndex = numHitConnections - numToTry; // Set startIndex and endIndex to clip out that many from the end of the numHitConnections list
            int endIndex   = numHitConnections;
            ttl1List.addAll(hitConnections.subList(startIndex, endIndex)); // Add them to the TTL 1 list
            return returnLists; // Return it
        }

        /*
         * otherwise, it's not very widely distributed content -- send
         * the query to all hit connections plus 3 TTL = 2 connections
         */

        // Compose the list to send our probe query with a TTL of 1 to all the ultrapeers that have query route tables our search passes through
        ttl1List.addAll(hitConnections);

        // Additionally, send our probe query with a TTL of 2 to up to 3 ultrapeers that have no query route tables
        addToList(ttl2List, oldConnections, missConnections, 3); // If there are less than 3 oldConnections, even include some missConnections

        // Return the lists we composed
        return returnLists;
    }

    /**
     * Add numElements from list1 and then list2 to listToAddTo.
     * 
     * Add the objects in list1 and list2 into listToAddTo.
     * Adds all the objects in list1 before moving into list2.
     * Stops after adding numElements objects.
     * 
     * @param listToAddTo The destination list
     * @param list1       First, add objects from list1 to listToAddTo
     * @param list2       When they have all been added, add objects from list2
     * @param numElements Don't add more objects than this
     */
    private static void addToList(List listToAddTo, List list1, List list2, int numElements) {

        // The first list has enough or more than enough elements
        if (list1.size() >= numElements) {

            // Add numElements from list1 to listToAddTo, and leave
            listToAddTo.addAll(list1.subList(0, numElements));
            return;

        // The first list doesn't have numElements, we'll add them all and then go into the second list
        } else {

            // Add everything from list1 into listToAddTo
            listToAddTo.addAll(list1);
        }

        // Find out how many more objects we can put in listToAddTo
        numElements = numElements - list1.size(); // We added all of list1

        // The second list has enough or more than enough elements
        if (list2.size() >= numElements) {

            // Fill the remaining space in listToAddTo with numElements
            listToAddTo.addAll(list2.subList(0, numElements));

        // The second list has less objects that we could take
        } else {

            // Add them all
            listToAddTo.addAll(list2);
        }
    }

    /**
     * Compose lists of ultrapeers for us to query in an agressive probe.
     * 
     * Helper method that creates lists of TTL=1 and TTL=2 connections to query
     * for an aggressive probe.  This is desired, for example, when the desired
     * file appears to be rare or when there is not enough data to determine
     * the file's popularity.
     * returns the array of TTL=1 and TTL=2 connections to query
     * 
     * @param oldConnections  Ultrapeers that don't have a query route table.
     * @param missConnections Ultrapeers with query route tables that block our search.
     * @param hitConnections  Ultrapeers with query route tables our search makes it through.
     * @param returnLists     An array of 2 LinkedList objects.
     *                        returnLists[0] contains up to 4 ultrapeers from the hitConnections list, our search passes through their query route tables.
     *                        returnLists[1] contains up to 3 ultrapeers from the oldConnections and missConnections lists.
     */
    private static LinkedList[] createAggressiveProbe(List oldConnections, List missConnections, List hitConnections, LinkedList[] returnLists) {

        /*
         * add as many connections as possible from first the old connections
         * list, then the connections that did not have hits
         */

        // Combine oldConnections and missConnections, and copy the first 3 connections into returnLists[1]
        addToList(returnLists[1], oldConnections, missConnections, 3); // Only goes into missConnections if there are fewer than 3 in oldConnections

        /*
         * add any hits there are to the TTL = 1 list
         */

        // Put up to 4 ultrapeers that have query route tables our search passes through in the returnLists[0] list
        int maxIndex = Math.min(4, hitConnections.size());
        returnLists[0].addAll(hitConnections.subList(0, maxIndex));

        // Return the array of 2 lists we filled
        return returnLists;
    }
}
