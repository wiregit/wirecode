package com.limegroup.gnutella.search;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.*;

/**
 * This class handles query "probes."  Probe queries are the initial queries
 * that are sent out to determine the popularity of the file.  This allows 
 * queries down new connections to have more information for choosing the TTL.
 */
final class ProbeQuery {
    
    /**
     * Constant list of hosts to probe query at ttl=1.
     */
    private final LinkedList TTL_1_PROBES;
    
    /**
     * Constant list of hosts to probe query at ttl=2.
     */
    private final LinkedList TTL_2_PROBES;

    /**
     * Constant reference to the query handler instance.
     */
    private final QueryHandler QUERY_HANDLER;

    /**
     * Constructs a new <tt>ProbeQuery</tt> instance with the specified
     * list of connections to query and with the data enclosed in the
     * <tt>QueryHandler</tt>.
     *
     * @param connections the <tt>List</tt> of connections to query
     * @param qh the <tt>QueryHandler</tt> instance containing data
     *  for the probe
     */
    ProbeQuery(List connections, QueryHandler qh) {
        QUERY_HANDLER = qh;
        LinkedList[] lists = 
            createProbeLists(connections, qh.QUERY);
        TTL_1_PROBES = lists[0];
        TTL_2_PROBES = lists[1];
        
        if(QUERY_HANDLER.QUERY.getHops() == 0) {
            System.out.println("ProbeQuery::ProbeQuery::"+
                               " ttl1: "+TTL_1_PROBES.size()+
                               " ttl2: "+TTL_2_PROBES.size()); 
        }
    }
    
    /**
     * Determines whether or not the probe is finished.
     *
     * @return <tt>true</tt> if the probe is finished, otherwise
     *  <tt>false</tt>
     */
    boolean finishedProbe() {
        return (TTL_1_PROBES.isEmpty() &&  TTL_2_PROBES.isEmpty());
    }

    /**
     * Obtains the time to wait for probe results to return.
     */
    long getTimeToWait() {
        return (TTL_1_PROBES.size() * 
                QueryHandler.TIME_TO_WAIT_PER_HOP) +
            (TTL_2_PROBES.size() * 2 * 
             QueryHandler.TIME_TO_WAIT_PER_HOP);
    }
    
    /**
     * Sends the next probe query out on the network if there 
     * are more to send.
     *
     * @return the number of hosts theoretically hit by this new 
     *  probe
     */
    int sendProbe() {
        if(QUERY_HANDLER.QUERY.getHops() == 0) {
            System.out.println("ProbeQuery::sendProbe"); 
        }
        Iterator iter = TTL_1_PROBES.iterator();
        int hosts = 0;
        QueryRequest query = QUERY_HANDLER.createQuery((byte)1);
        while(iter.hasNext()) {
            if(QUERY_HANDLER.QUERY.getHops() == 0) {
                System.out.println("ProbeQuery::sendProbe::TTL=1"); 
            }
            ManagedConnection mc = (ManagedConnection)iter.next();
            hosts += 
                QUERY_HANDLER.sendQueryToHost(query, 
                                              mc, QUERY_HANDLER);
        }
        
        query = QUERY_HANDLER.createQuery((byte)2);
        iter = TTL_2_PROBES.iterator();
        while(iter.hasNext()) {
            if(QUERY_HANDLER.QUERY.getHops() == 0) {
                System.out.println("ProbeQuery::sendProbe::TTL=2"); 
            }
            ManagedConnection mc = (ManagedConnection)iter.next();
            hosts += 
                QUERY_HANDLER.sendQueryToHost(query, 
                                              mc, QUERY_HANDLER);
        }
        
        TTL_1_PROBES.clear();
        TTL_2_PROBES.clear();
        return hosts;
    }

    /**
     * Helper method that creates the list of nodes to query for the probe.
     * This list will vary in size depending on how popular the content appears
     * to be.
     */
    private static LinkedList[] createProbeLists(List connections, QueryRequest query) {
        Iterator iter = connections.iterator();
        
        LinkedList missConnections = new LinkedList();
        LinkedList oldConnections  = new LinkedList();
        LinkedList hitConnections  = new LinkedList();
        while(iter.hasNext()) {
            ManagedConnection mc = (ManagedConnection)iter.next();
            
            if(mc.isGoodUltrapeer()) {
                //ManagedConnectionQueryInfo qi = mc.getQueryRouteState();
                
                //QueryRouteTable qrt = mc.getQueryKey
                //if(qi.lastReceived == null) continue;
                if(mc.hitsQueryRouteTable(query)) { 
                    hitConnections.add(mc);
                } else {
                    missConnections.add(mc);
                }
            } else {
                oldConnections.add(mc);
            }
        }
        if(query.getHops() == 0) {
            System.out.println(query.getQuery()+
                               " hitConnections:  "+hitConnections.size()+
                               " missConnections: "+missConnections.size()+
                               " oldConnections:  "+oldConnections.size()); 
        }
        // final list of connections to query
        LinkedList[] returnLists = new LinkedList[2];
        LinkedList ttl1List = new LinkedList();
        LinkedList ttl2List = new LinkedList();
        returnLists[0] = ttl1List;
        returnLists[1] = ttl2List;        

        // do we have adequate data to determine some measure of the file's popularity?
        boolean adequateData = 
            (missConnections.size()+hitConnections.size()) > 8;

        // if we don't have enough data from QRP tables, just send out a traditional probe
        // also, if we don't have an adequate number of QRP tables to access the 
        // popularity of the file, just send out an old-style probe at TTL=2
        if(hitConnections.size() == 0 || !adequateData) {
            return createAggressiveProbe(oldConnections, missConnections, 
                                         hitConnections, returnLists);
        } 

        int numHitConnections = hitConnections.size();
        double popularity = 
            (double)((double)numHitConnections/
                     ((double)missConnections.size()+numHitConnections));

        // if there were no matches, then it's almost definitely a fairly
        // rare file, so send out a more aggressive probe
        if(popularity == 0.0) {
            return createAggressiveProbe(oldConnections, missConnections, 
                                         hitConnections, returnLists);      
        }
        
        // if the file appears to be very popular, send it to only one host
        if(popularity == 1.0) {
            ttl1List.add(hitConnections.getFirst());
            return returnLists;
        }

        // mitigate the extremes of the popularity measurement a bit
        //popularity = popularity * 0.75;
        
        // the number of TTL=1 nodes we would hit if we had that many
        // connections with hits
        int idealTTL1ConnectionsToHit =
            Math.max(2, (int)(30.0 * (double)(1.0-popularity)));

        int realTTL1ConnectionsToHit =
            Math.min(numHitConnections, idealTTL1ConnectionsToHit);

        ttl1List.addAll(hitConnections.subList(0, realTTL1ConnectionsToHit));        

        // the "left over" number of nodes we need to hit after all
        // of our hit connections are used up.
        int extraNodesNeeded =
            idealTTL1ConnectionsToHit - realTTL1ConnectionsToHit;
        
        // add more TTL=2 nodes to the probe if we need them
        if(extraNodesNeeded > 0 && numHitConnections < 3) {
            if(extraNodesNeeded < 25) {
                addToList(ttl2List, oldConnections, missConnections, 1);
            } else {
                addToList(ttl2List, oldConnections, missConnections, 2);
            }
        }

        //System.out.println("popularity: "+popularity); 
        //System.out.println("idealTTL1ConnectionsToHit: "+idealTTL1ConnectionsToHit); 
        //System.out.println("extra nodes needed: "+extraNodesNeeded); 
        //System.out.println("ttl1ConnectionsToUse: "+ttl1ConnectionsToUse); 
        
        return returnLists;        
    }


    /**
     * Helper method that adds as many elements as possible up to the
     * desired number from two lists into a third list.  This method
     * takes as many elements as possible from <tt>list1</tt>, only
     * using elements from <tt>list2</tt> if the desired number of
     * elements to add cannot be fulfilled from <tt>list1</tt> alone.
     *
     * @param listToAddTo the list that elements should be added to
     * @param list1 the first list to add elements from, with priority 
     *  given to this list
     * @param list2 the second list to add elements from -- only used
     *  in the case where <tt>list1</tt> is smaller than <tt>numElements</tt>
     * @param numElements the desired number of elements to add to 
     *  <tt>listToAddTo</tt> -- note that this number will not be reached
     *  if the list1.size()+list2.size() < numElements
     */
    private static void addToList(List listToAddTo, List list1, List list2, 
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
     * Helper method that creates lists of TTL=1 and TTL=2 connections to query
     * for an aggressive probe.  This is desired, for example, when the desired
     * file appears to be rare or when there is not enough data to determine
     * the file's popularity.
     *
     * @param oldConnections the <tt>List</tt> of old-style connections
     * @param missConnections the <tt>List</tt> of new connections that did
     *  not have a hit for this query
     * @param hitConnections the <tt>List</tt> of connections with hits
     * @param returnLists the array of TTL=1 and TTL=2 connections to query
     */
    private static LinkedList[] 
        createAggressiveProbe(List oldConnections, List missConnections,
                              List hitConnections, LinkedList[] returnLists) {
        
        // add as many connections as possible from first the old connections
        // list, then the connections that did not have hits
        addToList(returnLists[1], oldConnections, missConnections, 3);

        // add any hits there are to the TTL=1 list
        int maxIndex = Math.min(4, hitConnections.size());
        returnLists[0].addAll(hitConnections.subList(0, maxIndex));

        return returnLists;        
    }
}













