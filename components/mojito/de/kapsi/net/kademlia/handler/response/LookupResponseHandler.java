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
    
    /**
     * This lock prevents beginNextRound() from being
     * executed before not all responses have arrived
     * or have timed out
     */
    private CountDownLock lock = new CountDownLock(0);

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
        
        // Select the A closest Nodes from the K bucket list
        List bucketList = context.getRouteTable().getBest(lookup, LookupSettings.getK());
        
        // Add the Nodes to the yet-to-be query list
        for(int i = bucketList.size()-1; i >= 0; i--) {
            addYetToBeQueried((Node)bucketList.get(i));
        }
        
        // Initial state: we're the closest Node to lookup
        markAsQueried(context.getLocalNode());
        closest = context.getLocalNodeID();
        
        lock = new CountDownLock(1);
    }
    
    public void lookup() throws IOException {
        beginNextRound();
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
                
                if (!isQueried(node) 
                        && !isYetToBeQueiried(node)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Adding " + node + " to the yet-to-be query list");
                    }
                    addYetToBeQueried(node);
                }
            }
            
            addResponse(new Node(nodeId, src));
            
            if (lock.countDown()) {
                beginNextRound();
            }
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
        
        if (lock.countDown()) {
            beginNextRound();
        }
    }
    
    private void beginNextRound() throws IOException {
        round++;
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting a new Lookup round #" + round + " for " + lookup);
        }
        
        Node best = (Node)responses.getBest(lookup);
        
        // We select A closest nodes on the first round!
        // Best node is null!
        boolean closer = (round == 1);
        
        if (best != null) {
            if (closest == null 
                    || !closest.equals(best.getNodeID())) {
                closest = best.getNodeID();
                closer = true;
            }
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("The last round #" + round + " brought us " 
                    + (closer ? "closer" : "NOT closer") + " to " + lookup);
        }
        
        if (!closer) {
            lookupFailures++;
        } else {
            lookupFailures = 0;
        }
        
        // TODO this (1st condition) requires some fine tuning. 
        // The current setting has been testet with maybe 200 Nodes.
        // The second condiation makes sure that we're connected
        // to at least 'k' Nodes or to all Nodes if there are in
        // total less than 'k' Nodes on the network.
        if (lookupFailures < LookupSettings.getMaxLookupFailures()
                || responses.size() < LookupSettings.getK()) {
            
            final int ka = closer ? LookupSettings.getA() : LookupSettings.getK();
            List bucketList = toQuery.getBest(lookup, queried, ka);
            final int size = bucketList.size();
            
            MessageDispatcher messageDispatcher 
                = context.getMessageDispatcher();
            
            lock = new CountDownLock(size);
            for(int i = 0; i < size; i++) {
                Node node = (Node)bucketList.get(i);
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sending " + node + " a Find request for " + lookup);
                }
                
                markAsQueried(node);
                messageDispatcher.send(node, createMessage(lookup), this);
            }
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Giving up to find " + lookup + " after " + round + " rounds and " + lookupFailures + " lookup errors");
            }
        }
        
        if (lock.getCount() <= 0) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookup + " terminates with " 
                        + best + " as the best match after " 
                        + round + " lookup rounds and " 
                        + queried.size() + " queried Nodes");
            }
            
            if (isValueLookup()) {
                finish(lookup, null, time);
            } else {
                // TODO optimize we don't need all features of getBest() 
                Collection nodes = responses.getBest(lookup, responses.size());
                finish(lookup, nodes, time);
            }
            
            finished = true;
            lock = new CountDownLock(Integer.MAX_VALUE);
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
    
    protected boolean isYetToBeQueiried(Node node) {
        return toQuery.containsKey(node.getNodeID());
    }
    
    protected void addResponse(Node node) {
        responses.put(node.getNodeID(), node);
    }
    
    protected List getClosestNodes(KUID lookup, int k) {
        return responses.getBest(lookup, k);
    }
    
    /**
     * Prevents beginNextRound() from being executed
     * before not all responses to our requests have 
     * either arrived or have timed out.
     */
    private static class CountDownLock {
        
        private int counter;
        
        public CountDownLock(int counter) {
            this.counter = counter;
        }
        
        public boolean countDown() {
            return --counter <= 0;
        }
        
        public int getCount() {
            return counter;
        }
        
        public String toString() {
            return "CountDownLock: " + counter;
        }
    }
}
