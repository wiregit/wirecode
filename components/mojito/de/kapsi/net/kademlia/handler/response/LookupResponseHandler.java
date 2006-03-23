/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
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
import de.kapsi.net.kademlia.settings.LookupSettings;
import de.kapsi.net.kademlia.util.PatriciaTrie;

/**
 * 
 */
public abstract class LookupResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupResponseHandler.class);
    
    /** The Key we're looking for */
    protected final KUID lookup;
    
    /** Set of queried KUIDs */
    private Set queried = new HashSet();
    
    /** Trie of <KUID,ContactNode> from whom we got replies */
    private PatriciaTrie responses = new PatriciaTrie();
    
    /** Trie of <KUID,ContactNode> to query */
    private PatriciaTrie toQuery = new PatriciaTrie();
    
    /** 
     * 
     */
    private Map queryKeys = new HashMap();
    
    /**
     * The number of lookup rounds
     */
    private int round = 0;
    
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
    
    public LookupResponseHandler(Context context, KUID lookup) {
        this(context, lookup, KademliaSettings.getReplicationParameter());
    }
    
    public LookupResponseHandler(Context context, KUID lookup, int resultSize) {
        super(context, LookupSettings.getTimeout());
        
        this.lookup = lookup;
        this.resultSize = resultSize;
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
        
        // VALUE lookup response
        if (isValueLookup() && message instanceof FindValueResponse) {
            
            FindValueResponse response = (FindValueResponse)message;
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(ContactNode.toString(nodeId, src) 
                        + " returned KeyValues for " 
                        + lookup + " after " 
                        + round + " rounds and " 
                        + queried.size() + " queried Nodes");
            }
            
            if (!finished) {
                finishValueLookup(lookup, response, time());
            }
            
            finished = true;
        }
        
        // NODE/VALUE lookup response
        if (!finished && message instanceof FindNodeResponse) {
            
            FindNodeResponse response = (FindNodeResponse)message;
            
            for(Iterator it = response.iterator(); it.hasNext(); ) {
                ContactNode node = (ContactNode)it.next();
                
                if (!isQueried(node) 
                        && !isYetToBeQueried(node)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Adding " + node + " to the yet-to-be query list");
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
            
            --activeSearches;
            lookupStep();
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
        
        if (finished) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookup + " is finished");
            }
            return;
        }
        
        this.time += time;
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactNode.toString(nodeId, dst) 
                    + " did not response to our Find request");
        }
        
        --activeSearches;
        lookupStep();
    }
    
    
    public void lookup() throws IOException {
        
        MessageDispatcher messageDispatcher 
            = context.getMessageDispatcher();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting a new Lookup for " + lookup);
        }
        
        // Select the K closest Nodes from the K bucket list
        int k = KademliaSettings.getReplicationParameter();
        List bucketList = context.getRouteTable().select(lookup, k, false, true);
        
        // Add the Nodes to the yet-to-be query list
        for(int i = bucketList.size()-1; i >= 0; i--) {
            addYetToBeQueried((ContactNode)bucketList.get(i));
        }
        
        //TODO mark: What happens if we really are the closest node to lookup??? -> we skip ourself for now?
        markAsQueried(context.getLocalNode());
        
        // Get the first round of alpha nodes
        List alphaList = toQuery.select(lookup, KademliaSettings.getLookupParameter());
        
        // send alpha requests
        for(int i = 0; i < alphaList.size(); i++) {
            ContactNode node = (ContactNode)alphaList.get(i);
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sending " + node + " a Find request for " + lookup);
            }
            
            markAsQueried(node);
            ++activeSearches;
            messageDispatcher.send(node, createMessage(lookup), this);
        }
    }
    
    private void lookupStep() throws IOException {
        
        if(toQuery.isEmpty() && activeSearches == 0) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookup + " terminates. No contacts left to query ");
            }
            
            if (isValueLookup()) {
                finishValueLookup(lookup, null, time);
            } else {
                Collection nodes = responses.select(lookup, responses.size());
                finishNodeLookup(lookup, nodes, queryKeys, time);
            }
            
            finished = true;
            return;
        }
        
        //stop if we have enough values and the yet-to-query list does not contain
        //a closer node to the target than the furthest away that we have.
        if(responses.size() == resultSize) {
            KUID furthest = lookup.invert();
            ContactNode worstResponse = (ContactNode)responses.select(furthest);
            ContactNode bestToQuery = (ContactNode)toQuery.select(lookup);

            if(bestToQuery == null || 
                    worstResponse.getNodeID().isCloser(bestToQuery.getNodeID(), lookup)) {
            
                ContactNode bestResponse = (ContactNode)responses.select(lookup);
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookup + " terminates with " 
                        + bestResponse + " as the best match after " 
                        + round + " lookup rounds and " 
                        + queried.size() + " queried Nodes");
                }
            
                if (isValueLookup()) {
                    finishValueLookup(lookup, null, time);
                } else {
                    Collection nodes = responses.select(lookup, responses.size());
                    finishNodeLookup(lookup, nodes, queryKeys, time);
                }
                finished = true;
                return;
            }
        }
        
        int numLookups = KademliaSettings.getLookupParameter() - activeSearches;
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
                ++activeSearches;
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
        //if the list is full discard the furthest node and put this one
        if(responses.size() == resultSize) {
            KUID furthest = lookup.invert();
            
            ContactNode worst = (ContactNode)responses.select(furthest);
            responses.remove(worst.getNodeID());
            queryKeys.remove(worst);
        } 
        responses.put(node.getNodeID(), node);
    }
}
