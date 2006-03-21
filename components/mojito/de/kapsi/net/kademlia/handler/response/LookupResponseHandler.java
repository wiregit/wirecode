/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.response.LookupResponse;
import de.kapsi.net.kademlia.routing.RoutingTable;
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
    protected Set queried = new HashSet();
    
    /** Trie of <KUID,ContactNode> from whom we got replies */
    protected PatriciaTrie responses = new PatriciaTrie();
    
    /** Trie of <KUID,ContactNode> to query */
    protected PatriciaTrie toQuery = new PatriciaTrie();
    
    /** The closest NodeID we currently know */
    protected KUID closest = null;
    
    /** 
     * The number of times a lookup round hasn't
     * brought us closer to the lookup KUID
     */
    protected int lookupFailures = 0;
    
    /**
     * The number of lookup rounds
     */
    protected int round = 0;
    
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
     * Whather or not this Lookup handler is used
     * for KeyValue lookups
     */
    private boolean valueLookup = false;
    
    /**
     * Wheather or not this lookup has terminated
     */
    private boolean finished = false;
    
    public LookupResponseHandler(Context context, KUID lookup, boolean valueLookup) {
        super(context, LookupSettings.getTimeout());
        this.lookup = lookup;
        this.valueLookup = valueLookup;
        //TODO for now
        this.resultSize = KademliaSettings.getReplicationParameter();
        // Initial state: we're the closest ContactNode to lookup
        closest = context.getLocalNodeID();
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
        
        LookupResponse response = (LookupResponse)message;
        boolean isKeyValueResponse = response.isKeyValueResponse();
        
        // VALUE lookup response
        if (isValueLookup() && isKeyValueResponse) {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(ContactNode.toString(nodeId, src) 
                        + " returned KeyValues for " 
                        + lookup + " after " 
                        + round + " rounds and " 
                        + queried.size() + " queried Nodes");
            }
            
            if (!finished) {
                finish(lookup, response, time());
            }
            
            finished = true;
        }
        
        // NODE/VALUE lookup response
        if (!finished && !isKeyValueResponse) {
            
            for(Iterator it = response.iterator(); it.hasNext(); ) {
                ContactNode node = (ContactNode)it.next();
                
                if (!isQueried(node) 
                        && !isYetToBeQueried(node)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Adding " + node + " to the yet-to-be query list");
                    }
                    
                    addYetToBeQueried(node);
                    //also add to our routing table
                    context.getRouteTable().add(node,false);
                }
            }
            
            addResponse(new ContactNode(nodeId, src));
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
        List bucketList = context.getRouteTable().select(lookup, KademliaSettings.getReplicationParameter(),false,true);
        
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
        
        if(toQuery.size()==0 && activeSearches == 0) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookup + " terminates. No contacts left to query ");
            }
            Collection nodes = responses.select(lookup, responses.size());
            finish(lookup, nodes, time);
            
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
                    worstResponse.getNodeID().isCloser(bestToQuery.getNodeID(),lookup)) {
            
                ContactNode bestResponse = (ContactNode)responses.select(lookup);
                
                if (LOG.isTraceEnabled()) {
                  LOG.trace("Lookup for " + lookup + " terminates with " 
                          + bestResponse + " as the best match after " 
                          + round + " lookup rounds and " 
                          + queried.size() + " queried Nodes");
                }
            
                if (isValueLookup()) {
                    finish(lookup, null, time);
                } else {
                    Collection nodes = responses.select(lookup, responses.size());
                    finish(lookup, nodes, time);
                }
                finished = true;
                return;
            }
        }
        
        int numLookups = KademliaSettings.getLookupParameter() - activeSearches;
        if(numLookups>0) {
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

    protected abstract void finish(KUID lookup, Collection results, long time);
   
    public long time() {
        return time;
    }
    
    public boolean isValueLookup() {
        return valueLookup;
    }
    
    protected void markAsQueried(ContactNode node) {
        queried.add(node.getNodeID());
        toQuery.remove(node.getNodeID());
    }
    
    protected boolean isQueried(KUID nodeId) {
        return queried.contains(nodeId);
    }
    
    protected boolean isQueried(ContactNode node) {
        return queried.contains(node.getNodeID());
    }
    
    protected void addYetToBeQueried(ContactNode node) {
        if(!queried.contains(node) && !node.equals(context.getLocalNode())) toQuery.put(node.getNodeID(), node);
    }
    
    protected boolean isYetToBeQueried(ContactNode node) {
        return toQuery.containsKey(node.getNodeID());
    }
    
    protected void addResponse(ContactNode node) {
        //if the list is full discard the furthest node and put this one
        if(responses.size() == resultSize) {
            KUID furthest = lookup.invert();
            responses.remove(((ContactNode)responses.select(furthest)).getNodeID());
        } 
        responses.put(node.getNodeID(), node);
    }
    
    protected List getClosestNodes(KUID lookup, int k) {
        return responses.select(lookup, k);
    }
    
}
