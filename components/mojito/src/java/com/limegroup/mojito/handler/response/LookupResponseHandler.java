/*
 * Mojito Distributed Hash Tabe (DHT)
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
 
package com.limegroup.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
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

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.LookupListener;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.FindNodeLookupStatisticContainer;
import com.limegroup.mojito.statistics.FindValueLookupStatisticContainer;
import com.limegroup.mojito.statistics.SingleLookupStatisticContainer;
import com.limegroup.mojito.util.KeyValueCollection;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.Trie;
import com.limegroup.mojito.util.TrieUtils;

/**
 * The LookupResponseHandler handles Node as well as Value
 * lookups.
 */
public class LookupResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(LookupResponseHandler.class);
    
    /** The key we're looking for */
    private KUID lookup;
    
    /** Inverted lookup key (furthest away) */
    private KUID furthest;
    
    /** The time when this lookup was started */
    private long startTime;
    
    /** Set of queried KUIDs */
    private Set<KUID> queried = new HashSet<KUID>();
    
    /** Trie of ContactNodes we're going to query */
    private Trie<KUID, ContactNode> toQuery = new PatriciaTrie<KUID, ContactNode>();
    
    /** Trie of ContactNodes that did respond */
    private Trie<KUID, ContactQueryKeyEntry> responses = new PatriciaTrie<KUID, ContactQueryKeyEntry>();
    
    /** A Map we're using to count the number of hops */
    private Map<KUID, Integer> hopMap = new HashMap<KUID, Integer>();
    
    /** The expected result set size (aka K) */
    private int resultSetSize;
    
    /** The total number of responses we got */
    private int responseCount = 0;
    
    /** The number of currently active searches */
    private int activeSearches = 0;
    
    /** Whether or not the lookup has finished */
    private boolean lookupFinished = false;
    
    /** The number of value locations we've found if this is a value lookup */
    private int foundValueLocs = 0;
    
    /** Global lookup timeout */
    private long lookupTimeout;
    
    /**
     * The statistics for this lookup
     */
    private SingleLookupStatisticContainer lookupStat;
    
    /**
     * Either a collection of KeyValueCollections or ContactNodes
     * depending on whether this is a Node or Value lookup.
     */
    private Collection found = Collections.EMPTY_LIST;
    
    public LookupResponseHandler(KUID lookup, Context context) {
        super(context);
        
        if (!lookup.isNodeID() && !lookup.isValueID()) {
            throw new IllegalArgumentException("Lookup ID bust be either a NodeID or ValueID");
        }
        
        this.lookup = lookup;
        this.furthest = lookup.invert();
        
        resultSetSize = KademliaSettings.REPLICATION_PARAMETER.getValue();
        setMaxErrors(0); // Don't retry on timeout - takes too long!

        if (isValueLookup()) {
            lookupTimeout = KademliaSettings.VALUE_LOOKUP_TIMEOUT.getValue();
            lookupStat = new FindValueLookupStatisticContainer(context, lookup);
        } else {
            lookupTimeout = KademliaSettings.NODE_LOOKUP_TIMEOUT.getValue();
            lookupStat = new FindNodeLookupStatisticContainer(context, lookup);
        }
        
        List bucketList = context.getRouteTable().select(lookup, resultSetSize, false, true);
        for(Iterator it = bucketList.iterator(); it.hasNext(); ) {
            addYetToBeQueried((ContactNode)it.next(), 1);
        }
        
        addResponse(new ContactQueryKeyEntry(context.getLocalNode()));
        markAsQueried(context.getLocalNode());
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
    
    public KUID getLookupID() {
        return lookup;
    }
    
    public boolean isValueLookup() {
        return lookup.isValueID();
    }
    
    public boolean isExhaustiveValueLookup() {
        return KademliaSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue();
    }
    
    public synchronized void start() throws IOException {
        startTime = System.currentTimeMillis();
        
        // Get the first round of alpha nodes and send them requests
        List<ContactNode> alphaList = TrieUtils.select(toQuery, lookup, KademliaSettings.LOOKUP_PARAMETER.getValue());
        
        int sent = 0;
        for(ContactNode node : alphaList) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sending " + node + " a Find request for " + lookup);
            }
            
            try {
                doLookup(node);
                sent++;
            } catch (SocketException err) {
                LOG.error("A SocketException occured", err);
            }
        }
        
        if (sent == 0) {
            finishLookup(-1L, 0);
        }
    }
    
    public void stop() {
        super.stop();
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stopping lookup for " + lookup);
        }
        
        finishLookup(-1L, 0);
    }
    
    public long time() {
        if (startTime > 0L) {
            return System.currentTimeMillis() - startTime;
        }
        return -1L;
    }
    
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        
        if (!isStopped() && activeSearches > 0) {
            lookupStat.addReply();
            activeSearches--;
            
            ContactNode node = message.getContactNode();
            int hop = hopMap.get(node.getNodeID()).intValue();
            
            if (message instanceof FindValueResponse) {
                if (isValueLookup()) {
                    handleFindValueResponse((FindValueResponse)message, time, hop);
                } else {
                    // Some Idot sent us a FIND_VALUE response for a
                    // FIND_NODE lookup! Ignore? We're losing one
                    // parallel lookup (temporarily) if we do nothing.
                    // I think it's better to kick off a new lookup
                    // now rather than to wait for a yet another
                    // response/lookup that would re-activate this one.
                    lookupStep(hop);
                }
            } else {
                handleFindNodeResponse((FindNodeResponse)message, time, hop);
            }
            
            if (activeSearches == 0) {
                finishLookup(time(), hop);
            }
        }
    }
    
    protected synchronized void timeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        
        if (!isStopped() && activeSearches > 0) {
            lookupStat.addTimeout();
            activeSearches--;
            
            if (LOG.isTraceEnabled()) {
                if (isValueLookup()) {
                    LOG.trace(ContactNode.toString(nodeId, dst) 
                            + " did not respond to our FIND_VALUE request");   
                } else {
                    LOG.trace(ContactNode.toString(nodeId, dst) 
                            + " did not respond to our FIND_NODE request");
                }
            }
        
            
            int hop = hopMap.get(nodeId).intValue();
            lookupStep(hop);
            
            if (activeSearches == 0) {
                finishLookup(time(), hop);
            }
        }
    }
    
    public void handleError(final KUID nodeId, final SocketAddress dst, final RequestMessage message, Exception e) {
        if (LOG.isErrorEnabled()) {
            if (isValueLookup()) {
                LOG.error("Sending a FIND_VALUE request to " + ContactNode.toString(nodeId, dst) + " failed", e);
            } else {
                LOG.error("Sending a FIND_NODE request to " + ContactNode.toString(nodeId, dst) + " failed", e);
            }
        }
        
        if (e instanceof SocketException) {
            context.fireEvent(new Runnable() {
                public void run() {
                    try {
                        timeout(nodeId, dst, message, -1L);
                    } catch (IOException err) {
                        LOG.error(err);
                    }
                }
            });
        }
        
        fireTimeout(nodeId, dst, message, -1L);
    }
    
    private void handleFindValueResponse(FindValueResponse response, long time, int hop) throws IOException {
        
        long totalTime = time();
        KeyValueCollection c = new KeyValueCollection(response);
        
        if (c.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(response.getContactNode()
                    + " returned an empty KeyValueCollection for " + lookup);
            }
            
            lookupStep(hop);
            
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace(response.getContactNode()
                        + " returned KeyValues for "
                        + lookup + " after "
                        + queried.size() + " queried Nodes and a total time of "
                        + totalTime + "ms");
            }
        
            if (foundValueLocs == 0) {
                lookupStat.setHops(hop, isValueLookup());
                lookupStat.setTime((int)totalTime, isValueLookup());
            }
            foundValueLocs++;
            
            if (found == Collections.EMPTY_LIST) {
                found = new ArrayList();
            }
            found.add(c);
            
            fireFound(c, totalTime);
            
            if (isExhaustiveValueLookup()) {
                lookupStep(hop);
            } else {
                lookupFinished = true;
            }
        }
    }

    private void handleFindNodeResponse(FindNodeResponse response, long time, int hop) throws IOException {
        
        if (lookupFinished) {
            return;
        }
        
        Collection<ContactNode> nodes = response.getNodes();
        for(ContactNode node : nodes) {
            
            if (!NetworkUtils.isValidSocketAddress(node.getSocketAddress())) {
                /*if (response.getNodeID().equals(node.getNodeID())) {
                    node.setSocketAddress(response.getSocketAddress());
                } else {*/
                    if (LOG.isErrorEnabled()) {
                        LOG.error(response.getContactNode() 
                                + " sent us a ContactNode with an invalid IP/Port " + node);
                    }
                    continue;
                //}
            }
            
            if (!isQueried(node) 
                    && !isYetToBeQueried(node)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Adding " + node + " to the yet-to-be queried list");
                }
                
                if (!lookupFinished) {
                    addYetToBeQueried(node, hop+1);
                }
                
                // Add them to the routing table as not alive
                // contacts. We're likely going to add them
                // anyways!
                context.getRouteTable().add(node, false);
            }
        }
        
        if (!nodes.isEmpty()) {
            addResponse(new ContactQueryKeyEntry(response));
        }
        
        lookupStep(hop);
    }
    
    private void lookupStep(int hop) throws IOException {
        
        if (lookupFinished) {
            return;
        }
        
        long totalTime = time();
        if (lookupTimeout > 0L && totalTime >= lookupTimeout) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookup + " terminates after "
                        + hop + " hops and " + totalTime + "ms due to timeout.");
            }

            // Setting activeSearches to 0 and returning 
            // from here will fire a finishLookup() event!
            activeSearches = 0;
            return;
        }
        
        if (activeSearches == 0) {
            
            // Finish if nothing left to query...
            if (toQuery.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookup + " terminates after "
                            + hop + " hops and " + totalTime + "ms. No contacts left to query.");
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
                
            // ...or if we found the target node
            } else if (!context.isLocalNodeID(lookup) 
                    && responses.containsKey(lookup)) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookup + " terminates after "
                            + hop + " hops. Found traget ID!");
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
            }
        }
        
        if (responses.size() >= resultSetSize) {
            KUID worst = responses.select(furthest).getKey().getNodeID();
            
            KUID best = null;            
            if (!toQuery.isEmpty()) {
                best = toQuery.select(lookup).getNodeID();
            }
            
            if (best == null || worst.isNearer(best, lookup)) {
                if (activeSearches == 0) {
                    if (LOG.isTraceEnabled()) {
                        ContactNode bestResponse = responses.select(lookup).getKey();
                        LOG.trace("Lookup for " + lookup + " terminates after "
                                + hop + " hops, " + totalTime + "ms and " + queried.size() 
                                + " queried Nodes with " + bestResponse + " as best match");
                    }
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
            }
        }
        
        int numLookups = KademliaSettings.LOOKUP_PARAMETER.getValue() - activeSearches;
        if (numLookups > 0) {
            List<ContactNode> toQueryList = TrieUtils.select(toQuery, lookup, numLookups);
            for (ContactNode node : toQueryList) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sending " + node + " a find request for " + lookup);
                }
                
                try {
                    doLookup(node);
                } catch (SocketException err) {
                    LOG.error("A SocketException occured", err);
                }
            }
        }
    }
    
    private void finishLookup(long time, int hop) {
        lookupFinished = true;

        if (time >= 0L) {
            if (isValueLookup()) {
                if (found.isEmpty()) {
                    ((FindValueLookupStatisticContainer)lookupStat).FIND_VALUE_FAILURE.incrementStat();
                } else {
                    ((FindValueLookupStatisticContainer)lookupStat).FIND_VALUE_OK.incrementStat();
                }
            } else {
                lookupStat.setHops(hop, false);
                lookupStat.setTime((int)time, false);
                
                // addResponse(ContactNode) limits the size of the
                // Trie to K and we can thus use the size method of it!
                found = TrieUtils.select(responses, lookup, responses.size());
            }
        }
        
        fireFinish(found, time);
    }
    
    /*protected void resend(KUID nodeId, 
            SocketAddress dst, RequestMessage message) throws IOException {
        
        // This method is never called if max errors is set to 0!
        // Setting max errors to something else than 0 in lookups
        // is algorithmically incorrect!
        super.resend(nodeId, dst, message);
    }

    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        super.handleTimeout(nodeId, dst, message, time);
    }*/
    
    private void doLookup(ContactNode node) throws IOException {
        markAsQueried(node);
        context.getMessageDispatcher().send(node, createRequest(node.getSocketAddress()), this);
        lookupStat.addRequest();
        activeSearches++;
    }
    
    private RequestMessage createRequest(SocketAddress address) {
        if (isValueLookup()) {
            return context.getMessageHelper().createFindValueRequest(address, lookup);
        } else {
            return context.getMessageHelper().createFindNodeRequest(address, lookup);
        }
    }
    
    /** Returns whether or not the Node has been queried */
    private boolean isQueried(ContactNode node) {
        return queried.contains(node.getNodeID());
    }
    
    /** Marks the Node as queried */
    private void markAsQueried(ContactNode node) {
        queried.add(node.getNodeID());
        toQuery.remove(node.getNodeID());
    }
    
    /** Returns whether or not the Node is in the to-query Trie */
    private boolean isYetToBeQueried(ContactNode node) {
        return toQuery.containsKey(node.getNodeID());
    }
    
    /** Adds the Node to the to-query Trie */
    private boolean addYetToBeQueried(ContactNode node, int hop) {
        if (!isQueried(node) 
                && !context.isLocalNodeID(node.getNodeID())) {
            toQuery.put(node.getNodeID(), node);
            hopMap.put(node.getNodeID(), new Integer(hop));
            return true;
        }
        return false;
    }
    
    /** Adds the ContactNodeEntry to the response Trie */
    private void addResponse(ContactQueryKeyEntry entry) {
        responses.put(entry.getKey().getNodeID(), entry);
        
        if (responses.size() > resultSetSize) {
            ContactNode worst = responses.select(furthest).getKey();
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
    
    private void fireFinish(final Collection c, final long time) {
        context.fireEvent(new Runnable() {
            public void run() {
                if (!isStopped()) {
                    for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                        LookupListener listener = (LookupListener)it.next();
                        listener.finish(lookup, c, time);
                    }
                }
            }
        });
    }
    
    /**
     * A simple implementation of Map.Entry to store <ContactNode, QueryKey>
     * tuples. The Key is the ContactNode and the QueryKey is the Value.
     * 
     * This class is immutable!
     */
    private static class ContactQueryKeyEntry 
            implements Map.Entry<ContactNode, QueryKey> {
        
        private ContactNode node;
        private QueryKey queryKey;
        
        private ContactQueryKeyEntry(ContactNode node) {
            this(node, null);
        }
        
        private ContactQueryKeyEntry(FindNodeResponse response) {
            this(response.getContactNode(), response.getQueryKey());
        }
        
        private ContactQueryKeyEntry(ContactNode node, QueryKey queryKey) {
            this.queryKey = queryKey;
            this.node = node;
        }
        
        public ContactNode getKey() {
            return node;
        }
        
        public QueryKey getValue() {
            return queryKey;
        }
        
        public QueryKey setValue(QueryKey qk) {
            throw new UnsupportedOperationException("This is an immutable class");
        }
        
        public String toString() {
            return node + ", queryKey: " + queryKey;
        }
    }
}
