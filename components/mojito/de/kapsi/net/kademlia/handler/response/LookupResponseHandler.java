/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.LookupStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.routing.RoutingTable;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.util.PatriciaTrie;

/**
 * 
 */
public abstract class LookupResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupResponseHandler.class);
    
    /** The Key we're looking for */
    protected final KUID lookup;
    
    /** The Key that is furthest away from the lookup Key */
    private final KUID furthest;
    
    /** Set of queried KUIDs */
    private Set queried = new HashSet();
    
    /** Trie of <KUID,ContactNode> from whom we got replies */
    private PatriciaTrie responses = new PatriciaTrie();
    
    /** Trie of <KUID,ContactNode> to query */
    private PatriciaTrie toQuery = new PatriciaTrie();
    
    /** 
<<<<<<< LookupResponseHandler.java
     * Map of ContactNode -> QueryKey
=======
     * A map of the query keys for this lookup
>>>>>>> 1.24.2.3
     */
    private Map queryKeys = new HashMap();
    
    /**
     * A map of the hops for this lookup
     */
    private Map hopMap = new HashMap();
    
    
    /**
     * The number of searches that are currently active
     */
    private int activeSearches = 0;
       
    /**
     * The number of closest nodes returned for a node lookup
     */
    private int resultSize = 0;

    /**
     * The duration of the lookup
     */
    private long time = 0L;
    
    /**
     * Wheather or not this lookup has terminated
     */
    private boolean finished = false;
    
    /**
     * A flag to make sure a lookup instance cannot be
     * started multiple times.
     */
    private boolean active = false;
    
    /**
     * The statistics for this lookup
     */
    protected LookupStatisticContainer lookupStat;
    
    /**
     * The global lookup timeout
     */
    private final long timeout;
    
    
    public LookupResponseHandler(Context context, KUID lookup, long timeout) {
        super(context);
        
        this.lookup = lookup;
        this.furthest = lookup.invert();
       
        this.resultSize = KademliaSettings.REPLICATION_PARAMETER.getValue();
        this.timeout = timeout;
    }
    
    public void handleResponse(KUID nodeId, SocketAddress src, 
            Message message, long time) throws IOException {
        
        if (!isQueried(nodeId)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Got an unrequested response from " 
                        + ContactNode.toString(nodeId, src) + " for " + lookup);
            }
            return;
        }
        
        this.time += time;
        lookupStat.addReply();
        
        // VALUE lookup response
        if (isValueLookup() && message instanceof FindValueResponse) {
                FindValueResponse response = (FindValueResponse)message;
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace(ContactNode.toString(nodeId, src) 
                            + " returned KeyValues for " 
                            + lookup + " after " 
                            + queried.size() + " queried Nodes");
                }
                
                if (!finished) {
                    finishValueLookup(lookup, response.getValues(), time());
                }
                finished = true;
        }
        
        // NODE/VALUE lookup response
        if (!finished && message instanceof FindNodeResponse) {
            
            FindNodeResponse response = (FindNodeResponse)message;
            
            Collection values = response.getValues();
            for(Iterator it = values.iterator(); it.hasNext(); ) {
                ContactNode node = (ContactNode)it.next();
                
                if (!isQueried(node) 
                        && !isYetToBeQueried(node)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Adding " + node + " to the yet-to-be queried list");
                    }
                    
                    addYetToBeQueried(node);
                    
                    // Add them to the routing table as not alive
                    // contacts. We're likely going to add them
                    // anyways!
                    context.getRouteTable().add(node, false);
                }
            }
            
            ContactNode node = new ContactNode(nodeId, src);
            addResponse(node);
            
            QueryKey queryKey = response.getQueryKey();
            if (queryKey != null) {
                queryKeys.put(node, queryKey);
            }
            
            int hop = ((Integer)hopMap.get(nodeId)).intValue();
            
            --activeSearches;
            lookupStep(++hop);
        }
    }
    
    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) throws IOException {
        
        RoutingTable routeTable = getRouteTable();
        routeTable.handleFailure(nodeId);
        
        if (!isQueried(nodeId)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("LookupRequestHandler did not query " 
                        + ContactNode.toString(nodeId, dst));
            }
            return;
        }

        lookupStat.addTimeout();
        if (finished) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookup + " is finished");
            }
            return;
        }
        
        this.time += time;
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactNode.toString(nodeId, dst) 
                    + " did not respond to our Find request");
        }
        
        int hop = ((Integer)hopMap.get(nodeId)).intValue();
        
        --activeSearches;
        lookupStep(hop);
    }
    
    /**
     * Starts the lookup
     */
    public synchronized void lookup() throws IOException {
        
        if (active) {
            if (LOG.isErrorEnabled()) {
                LOG.error("This lookup for " + lookup + " is already active!");
            }
            return;
        }
        
        active = true;
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting a new Lookup for " + lookup);
        }
        
        MessageDispatcher messageDispatcher = context.getMessageDispatcher();
        
        // Select the K closest Nodes from the K bucket list
        List bucketList = context.getRouteTable().select(lookup, resultSize, false, true);
        
        // Add the Nodes to the yet-to-be query list
        for(int i = bucketList.size()-1; i >= 0; i--) {
            addYetToBeQueried((ContactNode)bucketList.get(i));
        }
        
        markAsQueried(context.getLocalNode());
        addResponse(context.getLocalNode());
        
        // Get the first round of alpha nodes
        List alphaList = toQuery.select(lookup, KademliaSettings.LOOKUP_PARAMETER.getValue());
        
        // send alpha requests
        for(int i = 0; i < alphaList.size(); i++) {
            ContactNode node = (ContactNode)alphaList.get(i);
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sending " + node + " a Find request for " + lookup);
            }
            
            markAsQueried(node);
            hopMap.put(node.getNodeID(),new Integer(0));
            ++activeSearches;
            
            lookupStat.addRequest();
            messageDispatcher.send(node, createMessage(lookup), this);
        }
    }
    
    private void lookupStep(int hop) throws IOException {
        
        if(timeout>0 && (time > timeout)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookup + " terminates after "
                        + hop + " hops. Lookup timeout reached");
            }
            finishLookup(hop);
        }
        
        if(activeSearches == 0) {
            //stop if we have nothing more to query and no more active searches
            if(toQuery.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookup + " terminates after "
                            + hop + " hops. No contacts left to query ");
                }
                finishLookup(hop);
                return;
            } 
            //Finish if we found the target node
            else if((lookup!= context.getLocalNodeID()) && (responses.containsKey(lookup))) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookup + " terminates after "
                            + hop + " hops. Reached target ID! ");
                }
                finishLookup(hop);
                return;
            }
        }
        //stop if we have enough values and the yet-to-query list does not contain
        //a closer node to the target than the furthest away that we have
        //and this is the last of the last set of concurrent searches
        if(responses.size() == resultSize) {
            ContactNode worstResponse = (ContactNode)responses.select(furthest);
            ContactNode bestToQuery = (ContactNode)toQuery.select(lookup);

            if(bestToQuery == null || 
                    worstResponse.getNodeID().isCloser(bestToQuery.getNodeID(), lookup)) {
            
                
                if(activeSearches == 0) {
                    if (LOG.isTraceEnabled()) {
                        ContactNode bestResponse = (ContactNode)responses.select(lookup);
                        LOG.trace("Lookup for " + lookup + " terminates with " 
                                + bestResponse + " as the best match after "+
                                hop + " hops and "
                                + queried.size() + " queried Nodes");
                    }
                    finishLookup(hop);
                    return;
                } else {
                    return;
                }
            }
        } 
        int numLookups = KademliaSettings.LOOKUP_PARAMETER.getValue() - activeSearches;
        if(numLookups > 0) {
            List bucketList = toQuery.select(lookup, numLookups);
            final int size = bucketList.size();
            
            MessageDispatcher messageDispatcher = context.getMessageDispatcher();
            
            for(int i = 0; i < size; i++) {
                ContactNode node = (ContactNode)bucketList.get(i);
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sending " + node + " a Find request for " + lookup);
                }
                
                markAsQueried(node);
                hopMap.put(node.getNodeID(),new Integer(hop));
                ++activeSearches;
                lookupStat.addRequest();
                messageDispatcher.send(node, createMessage(lookup), this);
            }
        }
    }
    
    protected abstract Message createMessage(KUID lookup);

    protected abstract void finishValueLookup(KUID lookup, Collection keyValues, long time);
    
    protected abstract void finishNodeLookup(KUID lookup, Collection nodes, Map queryKeys, long time);
    
    protected abstract boolean isValueLookup();
    
    public long time() {
        return time;
    }
    
    private void finishLookup(int hop) {
        lookupStat.setHops(hop);
        lookupStat.setTime((int)time);
        finished = true;
        if (isValueLookup()) {
            finishValueLookup(lookup, null, time);
        } else {
            List nodes = responses.select(lookup, responses.size());
            finishNodeLookup(lookup, nodes, queryKeys, time);
        }
    }
    
    private void markAsQueried(ContactNode node) {
        queried.add(node.getNodeID());
        toQuery.remove(node.getNodeID());
    }
    
    private boolean isQueried(KUID nodeId) {
        return queried.contains(nodeId);
    }
    
    private boolean isQueried(ContactNode node) {
        return queried.contains(node.getNodeID());
    }
    
    private void addYetToBeQueried(ContactNode node) {
        if(!queried.contains(node) 
                && !node.equals(context.getLocalNode())) {
            toQuery.put(node.getNodeID(), node);
        }
    }
    
    private boolean isYetToBeQueried(ContactNode node) {
        return toQuery.containsKey(node.getNodeID());
    }
    
    private void addResponse(ContactNode node) {
        responses.put(node.getNodeID(), node);
        //if the list is full discard the furthest node and put this one
        if(responses.size() > resultSize) {
            ContactNode worst = (ContactNode)responses.select(furthest);
            responses.remove(worst.getNodeID());
            queryKeys.remove(worst);
        } 
    }
}
