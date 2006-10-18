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
import com.limegroup.gnutella.util.PatriciaTrie;
import com.limegroup.gnutella.util.Trie;
import com.limegroup.gnutella.util.TrieUtils;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.exceptions.DHTBackendException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.LookupRequest;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.ContactUtils;
import com.limegroup.mojito.util.EntryImpl;

/**
 * The LookupResponseHandler class handles the entire Kademlia 
 * lookup process. Subclasses implement lookup specific feautues
 * like the type of the lookup (FIND_NODE and FIND_VALUE) or
 * different lookup termintation conditions.
 * 
 * Think of the LookupResponseHandler as some kind of State-Machine.
 */
public abstract class LookupResponseHandler<V> extends AbstractResponseHandler<V> {

    private static final Log LOG = LogFactory.getLog(LookupResponseHandler.class);
    
    /** The ID we're looking for */
    protected KUID lookupId;
    
    /** The ID which is furthest away from the lookupId */
    protected KUID furthestId;
    
    /** Set of queried KUIDs */
    protected Set<KUID> queried = new HashSet<KUID>();
    
    /** Trie of Contacts we're going to query */
    protected Trie<KUID, Contact> toQuery 
        = new PatriciaTrie<KUID, Contact>(KUID.KEY_ANALYZER);
    
    /** Trie of Contacts that did respond */
    protected Trie<KUID, Entry<Contact,QueryKey>> responses 
        = new PatriciaTrie<KUID, Entry<Contact,QueryKey>>(KUID.KEY_ANALYZER);
    
    /** Collection of Contacts that collide with our Node ID */
    protected Collection<Contact> collisions = new HashSet<Contact>();
    
    /** A Map we're using to count the number of hops */
    private Map<KUID, Integer> hopMap = new HashMap<KUID, Integer>();
    
    /** The expected result set size (aka K) */
    private int resultSetSize;
    
    /** The number of responses we have received */
    private int responseCount = 0;
    
    /** The number of Nodes from our RouteTable that failed  */
    private int routeTableFailures = 0;
    
    /** The k-closest IDs we selected to start the lookup */
    private Set<KUID> routeTableNodes = new HashSet<KUID>();
    
    /** The total number of failed lookups */
    private int totalFailures = 0;
    
    /** The time when this lookup was started */
    private long startTime = -1L;
    
    /** The number of currently active (parallel) searches */
    private int activeSearches = 0;
    
    /** 
     * A Contact we must contact (it's usually a Contact that 
     * did respond to a Ping or whatsoever)
     */
    private Contact forcedContact;
    
    /** The current hop */
    private int currentHop = 0;
    
    /**
     * Whether or not this lookup tries to return k live nodes.
     * This will increase the size of the set of hosts to query.
     */
    private boolean selectAliveNodesOnly = false;
    
    LookupResponseHandler(Context context, KUID lookupId) {
        this(context, null, lookupId, -1);
    }
    
    LookupResponseHandler(Context context, Contact forcedContact, KUID lookupId) {
        this(context, forcedContact, lookupId, -1);
    }
    
    LookupResponseHandler(Context context, KUID lookupId, int resultSetSize) {
        this(context, null, lookupId, resultSetSize);
    }
    
    LookupResponseHandler(Context context, Contact forcedContact, KUID lookupId, int resultSetSize) {
        super(context);
        
        this.forcedContact = forcedContact;
        this.lookupId = lookupId;
        this.furthestId = lookupId.invert();
        
        if (resultSetSize < 0) {
            resultSetSize = KademliaSettings.REPLICATION_PARAMETER.getValue();
        }
        
        this.resultSetSize = resultSetSize;
        
        setMaxErrors(0); // Don't retry on timeout - takes too long!
    }
    
    /**
     * Returns the Key we're looking for
     */
    public KUID getLookupID() {
        return lookupId;
    }
    
    /**
     * Returns whether or not the lookup has timed out
     */
    protected abstract boolean isLookupTimeout(long time);
    
    /**
     * Returns the number of parallel lookups
     */
    protected abstract int getParallelLookups();
    
    /**
     * Returns the result set size
     */
    protected int getResultSetSize() {
        return resultSetSize;
    }
    
    /**
     * Returns the current hop
     */
    protected int getCurrentHop() {
        return currentHop;
    }
    
    /**
     * Returns the number of Nodes from our RouteTable that failed
     */
    protected int getRouteTableFailures() {
        return routeTableFailures;
    }
    
    /**
     * Return the total number of failures
     */
    protected int getTotalFailures() {
        return totalFailures;
    }
    
    @Override
    public long getElapsedTime() {
        if (startTime > 0L) {
            return System.currentTimeMillis() - startTime;
        }
        return -1L;
    }
    
    /**
     * Sets whether or not only alive Contacts from the local 
     * RouteTable should be used as the lookup start Set. The
     * default is false as lookups are an important tool to
     * refresh the local RouteTable but in some cases it's
     * useful to use 'guaranteed' alive Contacts.
     */
    public void setSelectAliveNodesOnly(boolean selectAliveNodesOnly) {
        this.selectAliveNodesOnly = selectAliveNodesOnly;
    }
    
    @Override
    protected synchronized void start() throws DHTException {
        super.start();
        
        // Get the closest Contacts from our RouteTable 
        // and add them to the yet-to-be queried list.
        List<Contact> nodes = null;
        if (selectAliveNodesOnly) {
            // Select twice as many Contacts which should guarantee that
            // we've k-closest Nodes at the end of the lookup
            nodes = context.getRouteTable().select(lookupId, 2 * getResultSetSize(), true);
        } else {
            nodes = context.getRouteTable().select(lookupId, getResultSetSize(), false);
        }
        
        for(Contact node : nodes) {
            addYetToBeQueried(node, currentHop+1);
            routeTableNodes.add(node.getNodeID());
        }
        
        // Mark the local node as queried 
        // (we did a lookup on our own RouteTable)
        addResponse(context.getLocalNode(), null);
        markAsQueried(context.getLocalNode());
        
        // Get the first round of alpha nodes and send them requests
        List<Contact> alphaList = TrieUtils.select(toQuery, lookupId, getParallelLookups());
        
        //optimimize the first lookup step if we have enough parallel lookup slots
        if(alphaList.size() >= 3) {
            //get the MRS node of the k closest nodes
            nodes = BucketUtils.sort(nodes);
            Contact mrs = BucketUtils.getMostRecentlySeen(nodes);
            if(!alphaList.contains(mrs) && !context.isLocalNode(mrs)) {
                // If list is full, remove last element and add the MRS node
                if (alphaList.size() >= getParallelLookups()) {
                    alphaList.remove(alphaList.size()-1);
                }
                alphaList.add(mrs);
            }
        }
        
        // Make sure the forcedContact is in the alpha list
        if (forcedContact != null 
                && !alphaList.contains(forcedContact)
                && !context.isLocalNode(forcedContact)) {
            
            alphaList.add(0, forcedContact);
            hopMap.put(forcedContact.getNodeID(), currentHop+1);
            
            if (alphaList.size() > getParallelLookups()) {
                alphaList.remove(alphaList.size()-1);
            }
        }
        
        // Go Go Go!
        startTime = System.currentTimeMillis();
        for(Contact node : alphaList) {
            try {
                sendLookupRequest(node);
            } catch (IOException err) {
                throw new DHTException(err);
            }
        }
        
        finishLookupIfDone();
    }

    @Override
    public synchronized void handleResponse(ResponseMessage response, long time) throws IOException {
        // Synchronizing this method so that response() doesn't get called
        // if the handler isDone() or isCancelled()
        super.handleResponse(response, time);
    }

    @Override
    protected synchronized void response(ResponseMessage message, long time) throws IOException {
        
        decrementActiveSearches();
        Contact contact = message.getContact();
        
        Integer hop = hopMap.remove(contact.getNodeID());
        if (hop == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Hop counter is messed up for " + contact);
            }
            hop = new Integer(0);
        }
        
        currentHop = hop.intValue();
        
        if (message instanceof FindNodeResponse) {
            FindNodeResponse response = (FindNodeResponse)message;
            
            Collection<Contact> nodes = response.getNodes();
            for(Contact node : nodes) {
                
                if (!ContactUtils.isValidContact(contact, node)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Dropping " + node);
                    }
                    continue;
                }
                
                // Make sure we're not mixing IPv4 and IPv6 addresses.
                // See RouteTableImpl.add() for more Info!
                if (!ContactUtils.isSameAddressSpace(context.getLocalNode(), node)) {
                    
                    // Log as ERROR so that we're not missing this
                    if (LOG.isErrorEnabled()) {
                        LOG.error(node + " is from a different IP address space than local Node");
                    }
                    continue;
                }
                
                if (ContactUtils.isLocalContact(context, node, collisions)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Dropping " + node);
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
                addResponse(contact, response.getQueryKey());
            }
        }
        
        nextLookupStep();
        finishLookupIfDone();
    }
    
    @Override
    public synchronized void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage request, long time) throws IOException {
        
        // Synchronizing this method so that timeout() doesn't get called
        // if the handler isDone() or isCancelled()
        super.handleTimeout(nodeId, dst, request, time);
    }

    @Override
    protected synchronized void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        
        decrementActiveSearches();
        if (LOG.isTraceEnabled()) {
            LOG.trace(ContactUtils.toString(nodeId, dst) 
                    + " did not respond to our " + message);
        }
        
        Integer hop = hopMap.remove(nodeId);
        if (hop == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Hop counter is messed up for " + ContactUtils.toString(nodeId, dst));
            }
            hop = new Integer(0);
        }
        
        if (routeTableNodes.contains(nodeId)) {
            routeTableFailures++;
        }
        
        totalFailures++;
        
        currentHop = hop.intValue();
        nextLookupStep();
        finishLookupIfDone();
    }
    
    @Override
    public synchronized void handleError(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        
        // Synchronizing this method so that error() doesn't get called
        // if the handler isDone() or isCancelled()
        super.handleError(nodeId, dst, message, e);
    }

    @Override
    protected synchronized void error(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        
        if (e instanceof SocketException && hasActiveSearches()) {
            try {
                timeout(nodeId, dst, message, -1L);
            } catch (IOException err) {
                LOG.error("IOException", err);
                
                if (hasActiveSearches() == false) {
                    setException(new DHTException(err));
                }
            }
        } else {
            setException(new DHTBackendException(nodeId, dst, message, e));
        }
    }
    
    /**
     * This method is the heart of the lookup process. It selects 
     * Contacts from the toQuery Trie and sends them lookup requests
     * until we find a Node with the given ID, the lookup times out
     * or there are no Contacts left to query. 
     */
    private void nextLookupStep() throws IOException {
        
        long totalTime = getElapsedTime();
        
        if (isLookupTimeout(totalTime)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lookup for " + lookupId + " terminates after "
                        + currentHop + " hops and " + totalTime + "ms due to timeout.");
            }

            killActiveSearches();
            // finishLookup() gets called if activeSearches is zero!
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
            // It is important to have finished all the active searches before
            // probing for this condition, because in the case of a bootstrap lookup
            // we are actually updating the routing tables of the nodes we contact.
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
        
        if (responses.size() >= getResultSetSize()) {
            KUID worst = responses.select(furthestId).getKey().getNodeID();
            
            KUID best = null;            
            if (!toQuery.isEmpty()) {
                best = toQuery.select(lookupId).getNodeID();
            }
            
            if (best == null || worst.isNearerTo(lookupId, best)) {
                if (!hasActiveSearches()) {
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
        
        int numLookups = getParallelLookups() - getActiveSearches();
        if (numLookups > 0) {
            List<Contact> toQueryList = TrieUtils.select(toQuery, lookupId, numLookups);
            for (Contact node : toQueryList) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sending " + node + " a find request for " + lookupId);
                }
                
                try {
                    sendLookupRequest(node);
                } catch (SocketException err) {
                    LOG.error("A SocketException occured", err);
                }
            }
        }
    }

    /**
     * Sends a lookup request to the given Contact
     */
    protected boolean sendLookupRequest(Contact node) throws IOException {
        LookupRequest request = createLookupRequest(node.getContactAddress());
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending " + node + " a " + request);
        }
        
        markAsQueried(node);
        boolean requestWasSent = context.getMessageDispatcher().send(node, request, this);
        
        if (requestWasSent) {
            incrementActiveSearches();
        }
        return requestWasSent;
    }
    
    /**
     * A factory method to create FIND_NODE or FIND_VALUE lookup
     * requests
     */
    protected abstract LookupRequest createLookupRequest(SocketAddress address);
    
    /**
     * Calls finishLookup() if the lookup isn't already
     * finished and there are no parallel searches active
     */
    private void finishLookupIfDone() {
        if (!isDone() && !isCancelled() && hasActiveSearches() == false) {
            finishLookup();
        }
    }
    
    /**
     * Called when the lookup finishes
     */
    protected abstract void finishLookup();
    
    /**
     * Increments the 'activeSearches' counter by one
     */
    private void incrementActiveSearches() {
        activeSearches++;
    }
    
    /**
     * Decrements the 'activeSearches' counter by one
     */
    private void decrementActiveSearches() {
        if (activeSearches == 0) {
            if (LOG.isErrorEnabled()) {
                LOG.error("ActiveSearches counter is already 0");
            }
            return;
        }
        
        activeSearches--;
    }
    
    /**
     * Sets the 'activeSearches' counter to zero
     */
    private void killActiveSearches() {
        activeSearches = 0;
    }
    
    /**
     * Returns the number of current number of active
     * searches
     */
    private int getActiveSearches() {
        return activeSearches;
    }
    
    /**
     * Returns whether or not there are currently any
     * searches active
     */
    private boolean hasActiveSearches() {
        return getActiveSearches() > 0;
    }
    
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
            
        if (isQueried(node)) {
            return false;
        }
        
        KUID nodeId = node.getNodeID();
        if (context.isLocalNodeID(nodeId)
                || context.isLocalContactAddress(node.getContactAddress())) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " has either the same NodeID or contact"
                        + " address as the local Node " + context.getLocalNode());
            }
            return false;
        }
        
        toQuery.put(nodeId, node);
        hopMap.put(nodeId, hop);
        return true;
    }
    
    /** Adds the Contact-QueryKey Tuple to the response Trie */
    private void addResponse(Contact node, QueryKey queryKey) {
        
        Entry<Contact,QueryKey> entry 
            = new EntryImpl<Contact,QueryKey>(node, queryKey, true);
        
        responses.put(node.getNodeID(), entry);
        
        // We're only interested in the k-closest
        // Contacts so remove the worst ones
        if (responses.size() > getResultSetSize()) {
            Contact worst = responses.select(furthestId).getKey();
            responses.remove(worst.getNodeID());
        }
        responseCount++;
    }
    
    public String toString() {
        long time = getElapsedTime();
        boolean timeout = isLookupTimeout(time);
        int activeSearches = getActiveSearches();
        
        return "Class: " + getClass().getName() 
            + ", lookup: " + lookupId 
            + ", time: " + time 
            + ", timeout: " + timeout 
            + ", activeSearches: " + activeSearches;
    }
}