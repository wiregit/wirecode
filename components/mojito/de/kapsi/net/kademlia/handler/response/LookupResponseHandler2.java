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

import com.limegroup.gnutella.dht.statistics.FindNodeLookupStatisticContainer;
import com.limegroup.gnutella.dht.statistics.FindValueLookupStatisticContainer;
import com.limegroup.gnutella.dht.statistics.SingleLookupStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.Context.LookupManager;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class LookupResponseHandler2 extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupResponseHandler2.class);
    
    private KUID lookup;
    private KUID furthest;
    
    private LookupManager lookupManager;
    
    private long startTime;
    private long stopTime;
    
    private Set queried = new HashSet();
    
    private PatriciaTrie toQuery = new PatriciaTrie();
    
    private PatriciaTrie responses = new PatriciaTrie();
    
    private Map hopMap = new HashMap();
    
    private int resultSetSize;
    
    private int responseCount = 0;
    
    private int activeSearches = 0;
    
    /**
     * The statistics for this lookup
     */
    private SingleLookupStatisticContainer lookupStat;
    
    private LookupResponseHandler2(KUID lookup, Context context, LookupManager lookupManager) {
        super(context);
        
        if (!lookup.isNodeID() && !lookup.isValueID()) {
            throw new IllegalArgumentException("Lookup ID bust be either a NodeID or ValueID");
        }
        
        this.lookup = lookup;
        this.furthest = lookup.invert();
        this.lookupManager = lookupManager;
        
        init();
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
    
    public long time() {
        return Math.max(0L, startTime - System.currentTimeMillis());
    }
    
    public void handleResponse(KUID nodeId, 
            SocketAddress src, ResponseMessage message, long time) throws IOException {
        
        lookupStat.addReply();
        
        if (message instanceof FindValueResponse) {
            handleFindValueResponse(nodeId, src, (FindValueResponse)message, time);
        } else {
            handleFindNodeResponse(nodeId, src, (FindNodeResponse)message, time);
        }
    }
    
    private void handleFindValueResponse(KUID nodeId, 
            SocketAddress src, FindValueResponse response, long time) throws IOException {
        
        if (!isValueLookup()) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Received a KeyValue even though this is not a KeyValue lookup!");
            }
            
            // TODO continue lookup and call listeners if lookup finishes
        } else {
            
        }
    }

    private void handleFindNodeResponse(KUID nodeId, 
            SocketAddress src, FindNodeResponse response, long time) throws IOException {
        
        int hop = ((Integer)hopMap.get(nodeId)).intValue();
        
        Collection nodes = response.getValues();
        for(Iterator it = nodes.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            
            if (!isQueried(node) && !isYetToBeQueried(node)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Adding " + node + " to the yet-to-be queried list");
                }
                
                addYetToBeQueried(node, hop+1);
                
                // Add them to the routing table as not alive
                // contacts. We're likely going to add them
                // anyways!
                context.getRouteTable().add(node, false);
            }
        }
        
        if (!nodes.isEmpty()) {
            addResponse(new ContactNode(nodeId, src));
        }
        
        --activeSearches;
        lookupStep(hop);
    }
    
    private void lookupStep(int hop) throws IOException {
        
    }
    
    protected void handleResend(KUID nodeId, 
            SocketAddress dst, Message message) throws IOException {
        super.handleResend(nodeId, dst, message);
    }

    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        super.handleTimeout(nodeId, dst, message, time);
    }
    
    protected void handleFinalTimeout(KUID nodeId, 
            SocketAddress dst, Message message) throws IOException {
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
}
