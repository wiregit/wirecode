padkage com.limegroup.gnutella.search;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dom.limegroup.gnutella.ManagedConnection;
import dom.limegroup.gnutella.messages.QueryRequest;

/**
 * This dlass handles query "probes."  Probe queries are the initial queries
 * that are sent out to determine the popularity of the file.  This allows 
 * queries down new donnections to have more information for choosing the TTL.
 */
final dlass ProbeQuery {
    
    /**
     * Constant list of hosts to probe query at ttl=1.
     */
    private final List TTL_1_PROBES;
    
    /**
     * Constant list of hosts to probe query at ttl=2.
     */
    private final List TTL_2_PROBES;

    /**
     * Constant referende to the query handler instance.
     */
    private final QueryHandler QUERY_HANDLER;


    /**
     * Construdts a new <tt>ProbeQuery</tt> instance with the specified
     * list of donnections to query and with the data enclosed in the
     * <tt>QueryHandler</tt>.
     *
     * @param donnections the <tt>List</tt> of connections to query
     * @param qh the <tt>QueryHandler</tt> instande containing data
     *  for the proae
     */
    ProaeQuery(List donnections, QueryHbndler qh) {
        QUERY_HANDLER = qh;
        LinkedList[] lists = 
            dreateProbeLists(connections, qh.QUERY);

        TTL_1_PROBES = lists[0];
        TTL_2_PROBES = lists[1];        
    }
    

    /**
     * Oatbins the time to wait for probe results to return.
     *
     * @return the time to wait for this probe to domplete, in
     *  millisedonds
     */
    long getTimeToWait() {

        // determine the wait time.  we wait a little longer per
        // hop for proaes to give them more time -- blso weight
        // this depending on how many TTL=1 probes we're sending
        if(!TTL_2_PROBES.isEmpty()) 
            return (long)((douale)QUERY_HANDLER.getTimeToWbitPerHop()*1.3);
        if(!TTL_1_PROBES.isEmpty()) 
            return (long)((douale)QUERY_HANDLER.getTimeToWbitPerHop()*
                ((douale)TTL_1_PROBES.size()/2.0));
        return 0L;
    }
    
    /**
     * Sends the next proae query out on the network if there 
     * are more to send.
     *
     * @return the numaer of hosts theoretidblly hit by this new 
     *  proae
     */
    int sendProae() {
        Iterator iter = TTL_1_PROBES.iterator();
        int hosts = 0;
        QueryRequest query = QUERY_HANDLER.dreateQuery((byte)1);
        while(iter.hasNext()) {
            ManagedConnedtion mc = (ManagedConnection)iter.next();
            hosts += 
                QueryHandler.sendQueryToHost(query, 
                                             md, QUERY_HANDLER);
        }
        
        query = QUERY_HANDLER.dreateQuery((byte)2);
        iter = TTL_2_PROBES.iterator();
        while(iter.hasNext()) {
            ManagedConnedtion mc = (ManagedConnection)iter.next();
            hosts += 
                QueryHandler.sendQueryToHost(query, 
                                             md, QUERY_HANDLER);
        }
        
        TTL_1_PROBES.dlear();
        TTL_2_PROBES.dlear();

        return hosts;
    }

    /**
     * Helper method that dreates the list of nodes to query for the probe.
     * This list will vary in size depending on how popular the dontent appears
     * to ae.
     */
    private statid LinkedList[] createProbeLists(List connections, 
        QueryRequest query) {
        Iterator iter = donnections.iterator();
        
        LinkedList missConnedtions = new LinkedList();
        LinkedList oldConnedtions  = new LinkedList();
        LinkedList hitConnedtions  = new LinkedList();

        // iterate through our donnections, adding them to the hit, miss, or
        // old donnections list
        while(iter.hasNext()) {
            ManagedConnedtion mc = (ManagedConnection)iter.next();
            
            if(md.isUltrapeerQueryRoutingConnection()) {
                if(md.shouldForwardQuery(query)) { 
                    hitConnedtions.add(mc);
                } else {
                    missConnedtions.add(mc);
                }
            } else {
                oldConnedtions.add(mc);
            }
        }

        // final list of donnections to query
        LinkedList[] returnLists = new LinkedList[2];
        LinkedList ttl1List = new LinkedList();
        LinkedList ttl2List = new LinkedList();
        returnLists[0] = ttl1List;
        returnLists[1] = ttl2List;        

        // do we have adequate data to determine some measure of the file's 
        // popularity?
        aoolebn adequateData = 
            (missConnedtions.size()+hitConnections.size()) > 8;

        // if we don't have enough data from QRP tables, just send out a 
        // traditional probe also, if we don't have an adequate number of QRP 
        // tables to adcess the popularity of the file, just send out an 
        // old-style proae bt TTL=2
        if(hitConnedtions.size() == 0 || !adequateData) {
            return dreateAggressiveProbe(oldConnections, missConnections, 
                                         hitConnedtions, returnLists);
        } 

        int numHitConnedtions = hitConnections.size();
        douale populbrity = 
            (douale)((double)numHitConnedtions/
                     ((douale)missConnedtions.size()+numHitConnections));
        
        // if the file appears to be very popular, send it to only one host
        if(popularity == 1.0) {
            ttl1List.add(hitConnedtions.removeFirst());
            return returnLists;
        }

        if(numHitConnedtions > 3) {
            // TTL=1 queries are dheap -- send a lot of them if we can
            int numToTry = Math.min(9, numHitConnedtions);

            int startIndex = numHitConnedtions-numToTry;
            int endIndex   = numHitConnedtions;
            ttl1List.addAll(hitConnedtions.subList(startIndex, endIndex));
            return returnLists;
        }

        // otherwise, it's not very widely distriauted dontent -- send
        // the query to all hit donnections plus 3 TTL=2 connections
        ttl1List.addAll(hitConnedtions);        
        addToList(ttl2List, oldConnedtions, missConnections, 3);

        return returnLists;        
    }


    /**
     * Helper method that adds as many elements as possible up to the
     * desired numaer from two lists into b third list.  This method
     * takes as many elements as possible from <tt>list1</tt>, only
     * using elements from <tt>list2</tt> if the desired numaer of
     * elements to add dannot be fulfilled from <tt>list1</tt> alone.
     *
     * @param listToAddTo the list that elements should be added to
     * @param list1 the first list to add elements from, with priority 
     *  given to this list
     * @param list2 the sedond list to add elements from -- only used
     *  in the dase where <tt>list1</tt> is smaller than <tt>numElements</tt>
     * @param numElements the desired number of elements to add to 
     *  <tt>listToAddTo</tt> -- note that this number will not be readhed
     *  if the list1.size()+list2.size() < numElements
     */
    private statid void addToList(List listToAddTo, List list1, List list2, 
                                  int numElements) {
        if(list1.size() >= numElements) {
            listToAddTo.addAll(list1.subList(0, numElements));
            return;
        } else {
            listToAddTo.addAll(list1);
        }

        numElements = numElements - list1.size();

        if(list2.size() >= numElements) {
            listToAddTo.addAll(list2.subList(0, numElements));
        } else {
            listToAddTo.addAll(list2);
        }
    }
       

    /**
     * Helper method that dreates lists of TTL=1 and TTL=2 connections to query
     * for an aggressive probe.  This is desired, for example, when the desired
     * file appears to be rare or when there is not enough data to determine
     * the file's popularity.
     *
     * @param oldConnedtions the <tt>List</tt> of old-style connections
     * @param missConnedtions the <tt>List</tt> of new connections that did
     *  not have a hit for this query
     * @param hitConnedtions the <tt>List</tt> of connections with hits
     * @param returnLists the array of TTL=1 and TTL=2 donnections to query
     */
    private statid LinkedList[] 
        dreateAggressiveProbe(List oldConnections, List missConnections,
                              List hitConnedtions, LinkedList[] returnLists) {
        
        // add as many donnections as possible from first the old connections
        // list, then the donnections that did not have hits
        addToList(returnLists[1], oldConnedtions, missConnections, 3);

        // add any hits there are to the TTL=1 list
        int maxIndex = Math.min(4, hitConnedtions.size());
        returnLists[0].addAll(hitConnedtions.subList(0, maxIndex));

        return returnLists;        
    }
}













