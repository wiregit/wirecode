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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.FindNodeLookupStatisticContainer;
import com.limegroup.gnutella.dht.statistics.FindValueLookupStatisticContainer;
import com.limegroup.gnutella.dht.statistics.SingleLookupStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.LookupListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class LookupResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupResponseHandler.class);
    
    private KUID lookup;
    private KUID furthest;
    
    private long startTime;
    
    private Set queried = new HashSet();
    
    private PatriciaTrie toQuery = new PatriciaTrie();
    
    private PatriciaTrie responses = new PatriciaTrie();
    
    private Map hopMap = new HashMap();
    
    private int resultSetSize;
    
    private int responseCount = 0;
    
    private int activeSearches = 0;
    
    private boolean finished = false;
    
    private List listeners = new ArrayList();
    
    /**
     * The statistics for this lookup
     */
    private SingleLookupStatisticContainer lookupStat;
    
    public LookupResponseHandler(KUID lookup, Context context) {
        super(context);
        
        if (!lookup.isNodeID() && !lookup.isValueID()) {
            throw new IllegalArgumentException("Lookup ID bust be either a NodeID or ValueID");
        }
        
        this.lookup = lookup;
        this.furthest = lookup.invert();
        
        init();
    }

    public void addLookupListener(LookupListener listener) {
        listeners.add(listener);
    }
    
    public void removeLookupListener(LookupListener listener) {
        listeners.remove(listener);
    }

    public LookupListener[] getPingListeners() {
        return (LookupListener[])listeners.toArray(new LookupListener[0]);
    }
    
    private void init() {
        
        resultSetSize = KademliaSettings.REPLICATION_PARAMETER.getValue();
        setMaxErrors(0); // Don't retry on timeout - takes too long!
        
        if (isValueLookup()) {
            setTimeout(KademliaSettings.VALUE_LOOKUP_TIMEOUT.getValue());
            lookupStat = new FindValueLookupStatisticContainer(context, lookup);
        } else {
            setTimeout(KademliaSettings.NODE_LOOKUP_TIMEOUT.getValue());
            lookupStat = new FindNodeLookupStatisticContainer(context, lookup);
        }
        
        List bucketList = context.getRouteTable().select(lookup, resultSetSize, false, true);
        for(Iterator it = bucketList.iterator(); it.hasNext(); ) {
            addYetToBeQueried((ContactNode)it.next(), 1);
        }
        
        addResponse(context.getLocalNode());
        markAsQueried(context.getLocalNode());
    }
    
    public KUID getLookupID() {
        return lookup;
    }
    
    public boolean isValueLookup() {
        return lookup.isValueID();
    }
    
    public void start() throws IOException {
        startTime = System.currentTimeMillis();
        
        // Get the first round of alpha nodes and send them requests
        List alphaList = toQuery.select(lookup, KademliaSettings.LOOKUP_PARAMETER.getValue());
        for(Iterator it = alphaList.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sending " + node + " a Find request for " + lookup);
            }
            
            doLookup(node);
        }
    }
    
    public void stop() {
        super.stop();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stopping lookup for " + lookup);
        }
    }
    
    public long time() {
        if (startTime > 0L) {
            return System.currentTimeMillis() - startTime;
        }
        return 0L;
    }
    
    protected void response(ResponseMessage message, long time) throws IOException {
        
        lookupStat.addReply();
        
        if (!isStopped()) {
            int hop = ((Integer)hopMap.get(message.getNodeID())).intValue();
            if (message instanceof FindValueResponse) {
                handleFindValueResponse((FindValueResponse)message, time, hop);
            } else {
                handleFindNodeResponse((FindNodeResponse)message, time, hop);
            }
        }
    }
    
    private void handleFindValueResponse(FindValueResponse response, long time, int hop) throws IOException {
        
        if (isValueLookup()) {
            long totalTime = time();
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(response.getContactNode()
                        + " returned KeyValues for "
                        + lookup + " after "
                        + queried.size() + " queried Nodes and a total time of "
                        + totalTime + "ms");
            }
            
            if (!finished) {
                lookupStat.setHops(hop);
                lookupStat.setTime((int)totalTime);
                ((FindValueLookupStatisticContainer)lookupStat).FIND_VALUE_OK.incrementStat();
            }
            finished = true;
            
            fireFound(response.getValues(), totalTime);
        } else {
            // Some Idot sent us a FIND_VALUE response for a
            // FIND_NODE lookup! Ignore?
        }
    }

    private void handleFindNodeResponse(FindNodeResponse response, long time, int hop) throws IOException {
        
        Collection nodes = response.getValues();
        for(Iterator it = nodes.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            
            if (!isQueried(node) && !isYetToBeQueried(node)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Adding " + node + " to the yet-to-be queried list");
                }
                
                if (!finished) {
                    addYetToBeQueried(node, hop+1);
                }
                
                // Add them to the routing table as not alive
                // contacts. We're likely going to add them
                // anyways!
                context.getRouteTable().add(node, false);
            }
        }
        
        if (!finished) {
            if (!nodes.isEmpty()) {
                addResponse(response.getContactNode());
            }
            
            activeSearches--;
            lookupStep(hop);
        }
    }
    
    protected void timeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        
        lookupStat.addTimeout();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactNode.toString(nodeId, dst) 
                    + " did not respond to our FIND request");
        }
        
        if (!isStopped()) {
            int hop = ((Integer)hopMap.get(nodeId)).intValue();
            activeSearches--;
            lookupStep(hop);
        }
    }
    
    private void lookupStep(int hop) throws IOException {
        
        long time = time();
        if (timeout() > 0L && time >= timeout()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookup + " terminates after "
                        + hop + " hops and " + time + "ms due to timeout.");
            }
            finishLookup(hop, time);
            return;
        } 
        
        if (activeSearches == 0) {
            if (toQuery.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookup + " terminates after "
                            + hop + " hops and " + time + "ms. No contacts left to query.");
                }
                finishLookup(hop, time);
                return;
            } else if (!context.isLocalNodeID(lookup) && responses.containsKey(lookup)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookup + " terminates after "
                            + hop + " hops. Found traget ID!");
                }
                finishLookup(hop, time);
                return;
            }
        }
        
        if (responses.size() >= resultSetSize) {
            KUID worst = ((ContactNode)responses.select(furthest)).getNodeID();
            
            KUID best = null;            
            if (!toQuery.isEmpty()) {
                best = ((ContactNode)toQuery.select(lookup)).getNodeID();
            }
            
            if (best == null || worst.isCloser(best, lookup)) {
                if (activeSearches == 0) {
                    if (LOG.isTraceEnabled()) {
                        ContactNode bestResponse = (ContactNode)responses.select(lookup);
                        LOG.trace("Lookup for " + lookup + " terminates after "
                                + hop + " hops, " + time + "ms and " + queried.size() 
                                + " queried Nodes with " + bestResponse + " as best match");
                    }
                    finishLookup(hop, time);
                }
                return;
            }
        }
        
        int numLookups = KademliaSettings.LOOKUP_PARAMETER.getValue() - activeSearches;
        if (numLookups > 0) {
            List bucketList = toQuery.select(lookup, numLookups);
            for(Iterator it = bucketList.iterator(); it.hasNext(); ) {
                ContactNode node = (ContactNode)it.next();
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sending " + node + " a find request for " + lookup);
                }
                
                doLookup(node);
            }
        }
    }
    
    private void finishLookup(int hop, long time) {
        lookupStat.setHops(hop);
        lookupStat.setTime((int)time);
        finished = true;
        
        if (isValueLookup()) {
            ((FindValueLookupStatisticContainer)lookupStat).FIND_VALUE_FAILURE.incrementStat();
            fireFound(Collections.EMPTY_LIST, time);
        } else {
            
            // addResponse(ContactNode) limits the size of the
            // Trie to K and we can thus use the size method of it!
            List nodes = responses.select(lookup, responses.size());
            fireFound(nodes, time);
        }
    }
    
    protected void resend(KUID nodeId, 
            SocketAddress dst, Message message) throws IOException {
        super.resend(nodeId, dst, message);
    }

    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        super.handleTimeout(nodeId, dst, message, time);
    }
    
    private void doLookup(ContactNode node) throws IOException {
        markAsQueried(node);
        context.getMessageDispatcher().send(node, createRequest(node.getSocketAddress()), this);
        lookupStat.addRequest();
        activeSearches++;
    }
    
    private RequestMessage createRequest(SocketAddress address) {
        if (isValueLookup()) {
            return context.getMessageFactory().createFindValueRequest(address, lookup);
        } else {
            return context.getMessageFactory().createFindNodeRequest(address, lookup);
        }
    }
    
    private boolean isQueried(ContactNode node) {
        return queried.contains(node.getNodeID());
    }
    
    private void markAsQueried(ContactNode node) {
        queried.add(node.getNodeID());
        toQuery.remove(node.getNodeID());
    }
    
    private boolean isYetToBeQueried(ContactNode node) {
        return toQuery.containsKey(node.getNodeID());
    }
    
    private void addYetToBeQueried(ContactNode node, int hop) {
        if (!isQueried(node) && context.isLocalNodeID(node.getNodeID())) {
            toQuery.put(node.getNodeID(), node);
            hopMap.put(node.getNodeID(), new Integer(hop));
        }
    }
    
    private void addResponse(ContactNode node) {
        responses.put(node.getNodeID(), node);
        if (responses.size() > resultSetSize) {
            ContactNode worst = (ContactNode)responses.select(furthest);
            responses.remove(worst.getNodeID());
            //hopMap.remove(node.getNodeID()); // TODO
        }
        responseCount++;
    }
    
    private void fireFound(final Collection c, final long time) {
        context.fireEvent(new Runnable() {
            public void run() {
                if (!isStopped()) {
                    for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                        LookupListener listener = (LookupListener)it.next();
                        listener.found(lookup, c, time);
                    }
                }
            }
        });
    }
}
