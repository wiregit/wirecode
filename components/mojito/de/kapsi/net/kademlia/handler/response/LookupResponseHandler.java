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

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Node;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.io.MessageDispatcher;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.response.LookupResponse;
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
    
    /** Trie of <KUID,Node> from whom we got replies */
    protected PatriciaTrie responses = new PatriciaTrie();
    
    /** Trie of <KUID,Node> to query */
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
        this.resultSize = LookupSettings.getK();
        // Initial state: we're the closest Node to lookup
        //TODO mark: What happens if we really are the closest node to lookup??? -> we skip ourself for now?
        markAsQueried(context.getLocalNode());
        closest = context.getLocalNodeID();
    }
    
    
    public void handleResponse(KUID nodeId, SocketAddress src, 
            Message message, long time) throws IOException {
        
        if (!isQueried(nodeId)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Got an unrequested response from " 
                        + Node.toString(nodeId, src) + " for " + lookup);
            }
            return;
        }
        
        this.time += time;
        
        LookupResponse response = (LookupResponse)message;
        boolean isKeyValueResponse = response.isKeyValueResponse();
        
        // VALUE lookup response
        if (isValueLookup() && isKeyValueResponse) {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(Node.toString(nodeId, src) 
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
                Node node = (Node)it.next();
                
                //ignore ourselve
                if(node.equals(context.getLocalNode()))continue;
                
                if (!isQueried(node) 
                        && !isYetToBeQueried(node)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Adding " + node + " to the yet-to-be query list");
                    }
                    
                    addYetToBeQueried(node);
                }
            }
            
            addResponse(new Node(nodeId, src));
            --activeSearches;
            lookupStep();
        }
    }
    
    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) throws IOException {
        
        if (!isQueried(nodeId)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("LookupRequestHandler did not query " 
                        + Node.toString(nodeId, dst));
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
            LOG.trace(Node.toString(nodeId, dst) 
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
        List bucketList = context.getRouteTable().getBest(lookup, LookupSettings.getK());
        
        // Add the Nodes to the yet-to-be query list
        for(int i = bucketList.size()-1; i >= 0; i--) {
            addYetToBeQueried((Node)bucketList.get(i));
        }
        
        // Get the first round of alpha nodes
        List alphaList = toQuery.getBest(lookup, queried, LookupSettings.getA());

        // send alpha requests
        for(int i = 0; i < alphaList.size(); i++) {
            Node node = (Node)alphaList.get(i);
            
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
            Collection nodes = responses.getBest(lookup, responses.size());
            finish(lookup, nodes, time);
            
            finished = true;
            return;
        }
        
        //stop if we have enough values and the yet-to-query list does not contain
        //a closer node to the target than the furthest away that we have.
        if(responses.size() == resultSize) {
            KUID furthest = lookup.invert();
            Node worstResponse = (Node)responses.getBest(furthest);
            Node bestToQuery = (Node)toQuery.getBest(lookup);
            if(worstResponse.getNodeID().isCloser(bestToQuery.getNodeID(),lookup)) {
            
                Node bestResponse = (Node)responses.getBest(lookup);
                
                if (LOG.isTraceEnabled()) {
                  LOG.trace("Lookup for " + lookup + " terminates with " 
                          + bestResponse + " as the best match after " 
                          + round + " lookup rounds and " 
                          + queried.size() + " queried Nodes");
                }
                
                Collection nodes = responses.getBest(lookup, responses.size());
                finish(lookup, nodes, time);
          
                finished = true;
                return;
            }
        }
        
        int numLookups = LookupSettings.getA() - activeSearches;
        if(numLookups>0) {
            
            List bucketList = toQuery.getBest(lookup, queried, numLookups);
            final int size = bucketList.size();
            
            MessageDispatcher messageDispatcher = context.getMessageDispatcher();
            
            for(int i = 0; i < size; i++) {
                Node node = (Node)bucketList.get(i);
                
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
    
    protected void markAsQueried(Node node) {
        queried.add(node.getNodeID());
    }
    
    protected boolean isQueried(KUID nodeId) {
        return queried.contains(nodeId);
    }
    
    protected boolean isQueried(Node node) {
        return queried.contains(node.getNodeID());
    }
    
    protected void addYetToBeQueried(Node node) {
        toQuery.put(node.getNodeID(), node);
    }
    
    protected boolean isYetToBeQueried(Node node) {
        return toQuery.containsKey(node.getNodeID());
    }
    
    protected void addResponse(Node node) {
        //if the list is full discard the furthest node and put this one
        if(responses.size() == resultSize) {
            KUID furthest = lookup.invert();
            responses.remove((KUID)responses.getBest(furthest));
        } 
        responses.put(node.getNodeID(), node);
    }
    
    protected List getClosestNodes(KUID lookup, int k) {
        return responses.getBest(lookup, k);
    }
    
}
