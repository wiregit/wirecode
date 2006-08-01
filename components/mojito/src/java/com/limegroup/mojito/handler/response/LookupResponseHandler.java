/*
 * Mojito Distributed Hash Table (Mojito DHT)
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.SingleLookupStatisticContainer;
import com.limegroup.mojito.util.ContactUtils;
import com.limegroup.mojito.util.EntryImpl;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.Trie;
import com.limegroup.mojito.util.TrieUtils;

/**
 * The LookupResponseHandler handles Node as well as Value
 * lookups.
 */
public abstract class LookupResponseHandler<V> extends AbstractResponseHandler<V> {
    
    private static final Log LOG = LogFactory.getLog(LookupResponseHandler.class);
    
    /** The key we're looking for */
    protected final KUID lookupId;
    
    /** Inverted lookup key (furthest away) */
    private KUID furthestId;
    
    /** The time when this lookup was started */
    private long startTime;
    
    /** Set of queried KUIDs */
    protected Set<KUID> queried = new HashSet<KUID>();
    
    /** Trie of Contacts we're going to query */
    private Trie<KUID, Contact> toQuery = new PatriciaTrie<KUID, Contact>();

    /** Trie of Contacts that did respond */
    protected Trie<KUID, Entry<Contact,QueryKey>> responses = new PatriciaTrie<KUID, Entry<Contact,QueryKey>>();
    
    /** A Map we're using to count the number of hops */
    private Map<KUID, Integer> hopMap = new HashMap<KUID, Integer>();
    
    protected int currentHop;
    
    /** The expected result set size (aka K) */
    private int resultSetSize;
    
    /** The total number of responses we got */
    private int responseCount = 0;
    
    /** The number of currently active searches */
    private int activeSearches = 0;
    
    /** Whether or not the lookup has finished */
    private boolean lookupFinished = false;
    
    /** Global lookup timeout */
    protected long lookupTimeout;
    
    /**
     * The statistics for this lookup
     */
    protected SingleLookupStatisticContainer lookupStat;
    
    private Contact force = null;
    
    LookupResponseHandler(Context context, KUID lookupId) {
        this(context, null, lookupId, -1);
    }
    
    LookupResponseHandler(Context context, KUID lookupId, int resultSetSize) {
        this(context, null, lookupId, resultSetSize);
    }
    
    LookupResponseHandler(Context context, Contact force, KUID lookupId) {
        this(context, force, lookupId, -1);
    }
    
    LookupResponseHandler(Context context, Contact force, KUID lookupId, int resultSetSize) {
        super(context);
        
        if (!lookupId.isNodeID() && !lookupId.isValueID()) {
            throw new IllegalArgumentException("Lookup ID bust be either a NodeID or ValueID");
        }
        
        this.force = force;
        this.lookupId = lookupId;
        this.furthestId = lookupId.invert();
        
        if (resultSetSize < 0) {
            resultSetSize = KademliaSettings.REPLICATION_PARAMETER.getValue();
        }
        
        this.resultSetSize = resultSetSize;
        
        setMaxErrors(0); // Don't retry on timeout - takes too long!

    }
    
    protected void init() {
    	List<Contact> nodes = context.getRouteTable().select(lookupId, resultSetSize, false);
        for(Contact node : nodes) {
            addYetToBeQueried(node, 1);
        }
        
        if (force != null) {
            addYetToBeQueried(force, 1);
        }
        
        Entry<Contact,QueryKey> entry 
            = new EntryImpl<Contact,QueryKey>(context.getLocalNode(), null, true);
        addResponse(entry);
        markAsQueried(context.getLocalNode());
    }
    
    public KUID getLookupID() {
        return lookupId;
    }
    
    @Override
    protected synchronized void start() throws Exception {
        startTime = System.currentTimeMillis();
        
        // Get the first round of alpha nodes and send them requests
        List<Contact> alphaList = TrieUtils.select(toQuery, lookupId, 
                                        KademliaSettings.LOOKUP_PARAMETER.getValue());
        
        if (force != null && !alphaList.contains(force)) {
            alphaList.add(0, force);
            alphaList.remove(alphaList.size()-1);
        }
        
        for(Contact node : alphaList) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sending " + node + " a Find request for " + lookupId);
            }
            
            doLookup(node);                
        }
        
        if (hasActiveSearches() == false) {
        	currentHop = 0;
            doFinishLookup(-1L);
        }
    }
    
    @Override
    public long time() {
        if (startTime > 0L) {
            return System.currentTimeMillis() - startTime;
        }
        return -1L;
    }
    
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        
        assert (hasActiveSearches());
        
        lookupStat.addReply();
        decrementActiveSearches();
        
        Contact node = message.getContact();
        currentHop = hopMap.remove(node.getNodeID()).intValue();
    }
    
    protected synchronized void timeout(KUID nodeId, 
            SocketAddress dst, RequestMessage message, long time) throws IOException {
        
        assert (hasActiveSearches());
        
        lookupStat.addTimeout();
        decrementActiveSearches();
        
        LOG.trace(ContactUtils.toString(nodeId, dst) 
                + " did not respond to our lookup request");
        
        currentHop = hopMap.remove(nodeId).intValue();
        lookupStep();
        
        postHandle();
    }
    
    protected void postHandle() {
    	if (!hasActiveSearches() || isLookupFinished()) {
            doFinishLookup(time());
        }
    }
    
    public void error(final KUID nodeId, final SocketAddress dst, final RequestMessage message, Exception e) {
        if (e instanceof SocketException && hasActiveSearches()) {
            try {
                timeout(nodeId, dst, message, -1L);
            } catch (IOException err) {
                LOG.error("IOException", err);
                
                if (!hasActiveSearches()) {
                    setException(err);
                }
            }
        } else {
            setException(e);
        }
    }

    protected void handleFindNodeResponse(FindNodeResponse response, long time) throws IOException {
        
        if (isLookupFinished()) {
            return;
        }
        
        Collection<Contact> nodes = response.getNodes();
        for(Contact node : nodes) {
            
            if (!NetworkUtils.isValidSocketAddress(node.getContactAddress())) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(response.getContact() 
                            + " sent us a ContactNode with an invalid IP/Port " + node);
                }
                continue;
            }
            
            if (context.isLocalNode(node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local node");
                }
                continue;
            }
            
            if (!isQueried(node) 
                    && !isYetToBeQueried(node)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Adding " + node + " to the yet-to-be queried list");
                }
                
                addYetToBeQueried(node, currentHop+1);
                
                // Add them to the routing table as not alive
                // contacts. We're likely going to add them
                // anyways!
                assert (node.isAlive() == false);
                context.getRouteTable().add(node);
            }
        }
        
        if (!nodes.isEmpty()) {
            Entry<Contact,QueryKey> entry 
                = new EntryImpl<Contact,QueryKey>(response.getContact(), response.getQueryKey(), true);
            addResponse(entry);
        }
        
        lookupStep();
    }
    
    protected void lookupStep() throws IOException {
        
        if (isLookupFinished()) {
            return;
        }
        
        long totalTime = time();
        if (lookupTimeout > 0L && totalTime >= lookupTimeout) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookupId + " terminates after "
                        + currentHop + " hops and " + totalTime + "ms due to timeout.");
            }

            // Setting activeSearches to 0 and returning 
            // from here will fire a finishLookup() event!
            setActiveSearches(0);
            return;
        }
        
        if (!hasActiveSearches()) {
            
            // Finish if nothing left to query...
            if (toQuery.isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookupId + " terminates after "
                            + currentHop + " hops and " + totalTime + "ms. No contacts left to query.");
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
                
            // ...or if we found the target node
            } else if (!context.isLocalNodeID(lookupId) 
                    && responses.containsKey(lookupId)) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Lookup for " + lookupId + " terminates after "
                            + currentHop + " hops. Found traget ID!");
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
            }
        }
        
        if (responses.size() >= resultSetSize) {
            KUID worst = responses.select(furthestId).getKey().getNodeID();
            
            KUID best = null;            
            if (!toQuery.isEmpty()) {
                best = toQuery.select(lookupId).getNodeID();
            }
            
            if (best == null || worst.isNearer(best, lookupId)) {
                if (activeSearches == 0) {
                    if (LOG.isTraceEnabled()) {
                        Contact bestResponse = responses.select(lookupId).getKey();
                        LOG.trace("Lookup for " + lookupId + " terminates after "
                                + currentHop + " hops, " + totalTime + "ms and " + queried.size() 
                                + " queried Nodes with " + bestResponse + " as best match");
                    }
                }
                
                // finishLookup() gets called if activeSearches is zero!
                return;
            }
        }
        
        int numLookups = KademliaSettings.LOOKUP_PARAMETER.getValue() - activeSearches;
        if (numLookups > 0) {
            List<Contact> toQueryList = TrieUtils.select(toQuery, lookupId, numLookups);
            for (Contact node : toQueryList) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sending " + node + " a find request for " + lookupId);
                }
                
                try {
                    doLookup(node);
                } catch (SocketException err) {
                    LOG.error("A SocketException occured", err);
                }
            }
        }
    }
    
    private boolean isLookupFinished() {
        return lookupFinished;
    }
    
    protected void setLookupFinished(boolean lookupFinished) {
        this.lookupFinished = lookupFinished;
    }
    
    protected abstract void doFinishLookup(long time);
    
    private boolean doLookup(Contact node) throws IOException {
        markAsQueried(node);
        
        boolean sent = context.getMessageDispatcher()
            .send(node, createRequest(node.getContactAddress()), this);
        
        if (sent) {
            lookupStat.addRequest();
            incrementActiveSearches();
        }
        return sent;
    }
    
    private void incrementActiveSearches() {
        activeSearches++;
        //System.out.println("inc: " + activeSearches);
    }
    
    private void decrementActiveSearches() {
        activeSearches--;
        //System.out.println("dec: " + activeSearches);
    }
    
    private void setActiveSearches(int activeSearches) {
        this.activeSearches = activeSearches;
        //System.out.println("set: " + activeSearches);
    }
    
    private boolean hasActiveSearches() {
        return activeSearches > 0;
    }
    
    protected abstract RequestMessage createRequest(SocketAddress address);
    
    /** Returns whether or not the Node has been queried */
    private boolean isQueried(Contact node) {
        return queried.contains(node.getNodeID());            
    }
    
    /** Marks the Node as queried */
    private void markAsQueried(Contact node) {
        queried.add(node.getNodeID());
        toQuery.remove(node.getNodeID());
    }
    
    /** Returns whether or not the Node is in the to-query Trie */
    private boolean isYetToBeQueried(Contact node) {
        return toQuery.containsKey(node.getNodeID());            
    }
    
    /** Adds the Node to the to-query Trie */
    private boolean addYetToBeQueried(Contact node, int hop) {
        if (!isQueried(node) && !context.isLocalNode(node)) {
            toQuery.put(node.getNodeID(), node);
            hopMap.put(node.getNodeID(), new Integer(hop));
            return true;
        }
        return false;
    }
    
    /** Adds the ContactNodeEntry to the response Trie */
    private void addResponse(Map.Entry<Contact, QueryKey> entry) {
        responses.put(entry.getKey().getNodeID(), entry);
        
        if (responses.size() > resultSetSize) {
            Contact worst = responses.select(furthestId).getKey();
            responses.remove(worst.getNodeID());
        }
        responseCount++;
    }
}