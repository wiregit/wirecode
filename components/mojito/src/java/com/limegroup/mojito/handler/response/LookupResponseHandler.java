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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import com.limegroup.gnutella.util.Trie.Cursor;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.exceptions.DHTBackendException;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.LookupRequest;
import com.limegroup.mojito.messages.MessageHelper;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.Contact;
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
abstract class LookupResponseHandler<V> extends AbstractResponseHandler<V> {
    
    private static final Log LOG = LogFactory.getLog(LookupResponseHandler.class);
    
    /**
     * The type of the lookup
     */
    public static enum LookupType {
        FIND_NODE,
        FIND_VALUE;
    }
    
    /** The ID we're looking for */
    private KUID lookupId;
    
    /** The ID which is furthest away from the lookupId */
    private KUID furthestId;
    
    /** Set of queried KUIDs */
    private Set<KUID> queried = new HashSet<KUID>();
    
    /** Trie of Contacts we're going to query */
    private Trie<KUID, Contact> toQuery 
        = new PatriciaTrie<KUID, Contact>(KUID.KEY_ANALYZER);
    
    /** Trie of Contacts that did respond */
    private Trie<KUID, Entry<Contact, QueryKey>> responses 
        = new PatriciaTrie<KUID, Entry<Contact, QueryKey>>(KUID.KEY_ANALYZER);
    
    /** A Map we're using to count the number of hops */
    private Map<KUID, Integer> hopMap = new HashMap<KUID, Integer>();
    
    /** The k-closest IDs we selected to start the lookup */
    private Set<KUID> routeTableNodes = new LinkedHashSet<KUID>();
    
    /** Collection of Contacts that collide with our Node ID */
    private Collection<Contact> collisions = new LinkedHashSet<Contact>();
    
    /** Collection of FindValueResponses if this is a FIND_VALUE lookup  */
    private Collection<FindValueResponse> valueResponses = null;
    
    /** A Set of Contacts that have the same Node ID as the local Node */
    private Set<Contact> forcedContacts = new LinkedHashSet<Contact>();

    /** The number of currently active (parallel) searches */
    private int activeSearches = 0;
    
    /** The number of responses we have received */
    private int responseCount = 0;
    
    /** The current hop */
    private int currentHop = 0;
    
    /** The expected result set size (aka K) */
    private int resultSetSize;
    
    /** The number of parallel lookups */
    private int parellelism;

    /** The type of the lookup */
    private LookupType lookupType;
    
    /**
     * Whether or not this lookup tries to return k live nodes.
     * This will increase the size of the set of hosts to query.
     */
    private boolean selectAliveNodesOnly = false;
    
    /** Whether or not this is an exhaustive lookup. */
    private boolean exchaustive = false;
    
    /** The time when this lookup started */
    private long startTime = -1L;
    
    /** The number of Nodes from our RouteTable that failed  */
    private int routeTableFailures = 0;
    
    /** The total number of failed lookups */
    private int totalFailures = 0;
    
    /** A flag that indicates whether or not the lookup has finished */
    //private boolean finished = false;
    
    /** 
     * Whether or not the (k+1)-closest Contact should be removed 
     * from the response Set 
     */
    private boolean deleteFurthest = true;
    
    /** A lock to manage parallel lookups */
    private Object lock = new Object();
    
    public LookupResponseHandler(Context context, LookupType lookupType, KUID lookupId) {
        super(context);
        
        this.lookupType = lookupType;
        this.lookupId = lookupId;
        this.furthestId = lookupId.invert();
        
        setMaxErrors(0); // Don't retry on timeout - takes too long!
        setParallelism(-1); // Default number of parallel lookups
        setResultSetSize(-1); // Default result set size
        setDeleteFurthest(KademliaSettings.DELETE_FURTHEST_CONTACT.getValue());
    }
    
    /**
     * Returns the type of the lookup
     */
    public LookupType getLookupType() {
        return lookupType;
    }
    
    /**
     * Returns true if this is a FIND_NODE lookup
     */
    public boolean isNodeLookup() {
        return lookupType.equals(LookupType.FIND_NODE);
    }
    
    /**
     * Returns true if this is a FIND_VALUE lookup
     */
    public boolean isValueLookup() {
        return lookupType.equals(LookupType.FIND_VALUE);
    }
    
    /**
     * Returns the Key we're looking for
     */
    public KUID getLookupID() {
        return lookupId;
    }
    
    /**
     * Sets the result set size
     */
    public void setResultSetSize(int resultSetSize) {
        if (resultSetSize < 0) {
            this.resultSetSize = KademliaSettings.REPLICATION_PARAMETER.getValue();
        } else if (resultSetSize > 0) {
            this.resultSetSize = resultSetSize;
        }
        
        throw new IllegalArgumentException("resultSetSize=" + resultSetSize);
    }
    
    /**
     * Returns the result set size
     */
    public int getResultSetSize() {
        return resultSetSize;
    }
    
    /**
     * Sets the number of parallel lookups this handler
     * should maintain
     */
    public void setParallelism(int parellelism) {
        if (parellelism < 0) {
            switch(getLookupType()) {
                case FIND_NODE:
                    this.parellelism  = KademliaSettings.FIND_NODE_PARALLEL_LOOKUPS.getValue();
                    break;
                case FIND_VALUE:
                    this.parellelism = KademliaSettings.FIND_VALUE_PARALLEL_LOOKUPS.getValue();
                    break;
                default:
                    throw new IllegalStateException(toString());
            }
        } else if (parellelism > 0) {
            this.parellelism = parellelism;
        }
        
        throw new IllegalArgumentException("parellelism=" + parellelism);
    }
    
    /**
     * Returns the number of parallel lookups this handler
     * maintains
     */
    public int getParallelism() {
        return parellelism;
    }
    
    /**
     * Adds the given Contact to the collection of Contacts
     * that must be contacted during the lookup
     */
    public void addForcedContact(Contact node) {
        forcedContacts.add(node);
    }
    
    /**
     * Returns an unmodifiyable collection of Contacts
     * that must be conacted during the lookup
     */
    public Collection<Contact> getForcedContacts() {
        return Collections.unmodifiableSet(forcedContacts);
    }
    
    /**
     * Sets whether or not this is an exhaustive lookup
     * (works only with FIND_VALUE lookups)
     */
    public void setExhaustive(boolean exchaustive) {
        if (isValueLookup()) {
            this.exchaustive = exchaustive;
        }
    }
    
    /**
     * Returns whether or not this is an exhaustive lookup
     */
    public boolean isExhaustive() {
        return exchaustive;
    }
    
    /**
     * Returns the current hop
     */
    public int getCurrentHop() {
        return currentHop;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.AbstractResponseHandler#getElapsedTime()
     */
    @Override
    public long getElapsedTime() {
        if (startTime > 0L) {
            return System.currentTimeMillis() - startTime;
        }
        return -1L;
    }
    
    /**
     * Returns the number of Nodes from our RouteTable that failed
     */
    public int getRouteTableFailures() {
        return routeTableFailures;
    }
    
    /**
     * Returns true if this lookup has timed out
     */
    private boolean isTimeout(long time) {
        long lookupTimeout = -1L;
        switch(getLookupType()) {
            case FIND_NODE:
                lookupTimeout = KademliaSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue();
                break;
            case FIND_VALUE:
                lookupTimeout = KademliaSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue();
                break;
            default:
                throw new IllegalStateException(toString());
        }
        
        return lookupTimeout > 0L && time >= lookupTimeout;
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
    
    /**
     * Returns whether or not onlt alive Contacts should be
     * selected for the first hop
     */
    public boolean isSelectAliveNodesOnly() {
        return selectAliveNodesOnly;
    }
    
    /**
     * Sets whether or not the furthest of the (k+1)-closest Contacts
     * that did respond should be deleted from the response Set.
     * This is primarly a memory optimization as we're only intersted
     * in the k-closest Contacts.
     * 
     * For caching we need the lookup path though (that means we'd set
     * this to false).
     */
    public void setDeleteFurthest(boolean deleteFurthest) {
        this.deleteFurthest = deleteFurthest;
    }
    
    /**
     * Returns whether or not the furthest of the (k+1)-closest
     * Contacts will be removed from the response Set.
     */
    public boolean isDeleteFurthest() {
        return deleteFurthest;
    }
    
    @Override
    protected void start() throws DHTException {
        super.start();
        
        // Get the closest Contacts from our RouteTable 
        // and add them to the yet-to-be queried list.
        List<Contact> nodes = null;
        if (isSelectAliveNodesOnly()) {
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
        List<Contact> alphaList = TrieUtils.select(toQuery, lookupId, getParallelism());
        
        //optimimize the first lookup step if we have enough parallel lookup slots
        if(alphaList.size() >= 3) {
            //get the MRS node of the k closest nodes
            nodes = BucketUtils.sort(nodes);
            Contact mrs = BucketUtils.getMostRecentlySeen(nodes);
            if(!alphaList.contains(mrs) && !context.isLocalNode(mrs)) {
                // If list is full, remove last element and add the MRS node
                if (alphaList.size() >= getParallelism()) {
                    alphaList.remove(alphaList.size()-1);
                }
                alphaList.add(mrs);
            }
        }
        
        // Make sure the forced Contacts are in the alpha list
        for (Contact forced : forcedContacts) {
            if (!alphaList.contains(forced)) {
                alphaList.add(0, forced);
                hopMap.put(forced.getNodeID(), currentHop+1);
                
                int last = alphaList.size()-1;
                if (alphaList.size() > getParallelism() 
                        && !forcedContacts.contains(alphaList.get(last))) {
                    alphaList.remove(last);
                }
            }
        }
        
        // Go Go Go!
        startTime = System.currentTimeMillis();  
        synchronized (lock) {
            for(Contact node : alphaList) {
                try {
                    sendLookupRequest(node);
                } catch (IOException err) {
                    throw new DHTException(err);
                }
            }
            
            finishLookupIfDone();
        }
    }

    @Override
    public void handleResponse(ResponseMessage response, long time) throws IOException {
        synchronized (lock) {
            super.handleResponse(response, time);
        }
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
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
        
        boolean nextStep = false;
        if (message instanceof FindNodeResponse) {
            nextStep = handleFindNodeResponse((FindNodeResponse)message);
        } else if (message instanceof FindValueResponse) {
            nextStep = handleFindValueResponse((FindValueResponse)message);
        }
        
        if (nextStep) {
            nextLookupStep();
        }
        
        finishLookupIfDone();
    }
    
    private boolean handleFindNodeResponse(FindNodeResponse response) throws IOException {
        Contact sender = response.getContact();
        Collection<Contact> nodes = response.getNodes();
        for(Contact node : nodes) {
            
            if (!ContactUtils.isValidContact(sender, node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Dropping invalid Contact: " + node);
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
                    LOG.info("Dropping colliding Contact: " + node);
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
            addResponse(sender, response.getQueryKey());
        }
        
        return true;
    }
    
    private boolean handleFindValueResponse(FindValueResponse response) throws IOException {
        Contact sender = response.getContact();
        
        if (!isValueLookup()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(sender + " sent an illegal response " + response 
                        + " for a " + getLookupType() + " lookup");
            }
            return true;
        }
        
        Collection<KUID> keys = response.getKeys();
        Collection<DHTValue> values = response.getValues();
        
        if (keys.isEmpty() && values.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(sender + " returned neither keys nor values for " + lookupId);
            }
            
            // Continue with the lookup...
            return true;
        }
        
        if (valueResponses == null) {
            valueResponses = new ArrayList<FindValueResponse>();
        }
        
        valueResponses.add(response);
        
        // Terminate the FIND_VALUE lookup if it isn't
        // an exhaustive lookup
        if (!isExhaustive()) {
            killActiveSearches();
            return false;
        }
        
        // Continue otherwise...
        return true;
    }
    
    @Override
    public void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage request, long time) throws IOException {
        synchronized (lock) {
            super.handleTimeout(nodeId, dst, request, time);            
        }
    }

    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, 
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
    public void handleError(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        synchronized (lock) {
            super.handleError(nodeId, dst, message, e);            
        }
    }

    @Override
    protected void error(KUID nodeId, SocketAddress dst, 
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
        
        if (isTimeout(totalTime)) {
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
        
        int numLookups = getParallelism() - getActiveSearches();
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
        SocketAddress addr = node.getContactAddress();
        
        MessageHelper messageHelper = context.getMessageHelper();
        LookupRequest request = null;
        switch(getLookupType()) {
            case FIND_NODE:
                request = messageHelper.createFindNodeRequest(addr, lookupId);
                break;
            case FIND_VALUE:
                Collection<KUID> noKeys = Collections.emptySet();
                request = messageHelper.createFindValueRequest(addr, lookupId, noKeys);
                break;
            default:
                throw new IllegalStateException(toString());
        }
        
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
     * Calls finishLookup() if the lookup isn't already
     * finished and there are no parallel searches active
     */
    private void finishLookupIfDone() {
        if (!isDone() && !isCancelled() && !hasActiveSearches()) {
            //finished = true;
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
        if (isDeleteFurthest() && responses.size() > getResultSetSize()) {
            Contact worst = responses.select(furthestId).getKey();
            responses.remove(worst.getNodeID());
        }
        responseCount++;
    }
    
    /**
     * Returns the k-closest Contacts sorted by their closeness
     * to the given lookup key
     */
    public Map<Contact, QueryKey> getNearestContacts() {
        return getContacts(getResultSetSize());
    }
    
    /**
     * Returns count number of Contacts sorted by their closeness
     * to the given lookup key
     */
    public Map<Contact, QueryKey> getContacts(int count) {
        if (count < 0) {
            count = responses.size();
        }
        
        final int maxCount = count;
        
        // Use a LinkedHashMap which preserves the insertion order...
        final Map<Contact, QueryKey> nearest = new LinkedHashMap<Contact, QueryKey>();
        
        responses.select(lookupId, new Cursor<KUID, Entry<Contact,QueryKey>>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Entry<Contact, QueryKey>> entry) {
                Entry<Contact, QueryKey> e = entry.getValue();
                nearest.put(e.getKey(), e.getValue());
                
                if (nearest.size() < maxCount) {
                    return SelectStatus.CONTINUE;
                }
                
                return SelectStatus.EXIT;
            }
        });
        
        return nearest;
    }
    
    /**
     * Returns a Collection of FindValueResponse if this was 
     * a FIND_VALUE lookup
     */
    public Collection<FindValueResponse> getValues() {
        if (!isValueLookup()) {
            throw new UnsupportedOperationException("This is not a FIND_VALUE lookup: " + getLookupType());
        }
        
        if (valueResponses != null) {
            return valueResponses;
        }
        return Collections.emptyList();
    }
    
    /**
     * Returns a Collection of Contacts that did collide with the
     * local Node ID
     */
    public Collection<Contact> getCollisions() {
        return collisions;
    }
    
    public String toString() {
        long time = getElapsedTime();
        boolean timeout = isTimeout(time);
        int activeSearches = getActiveSearches();
        
        return "Type: " + getLookupType() 
            + ", lookup: " + lookupId 
            + ", time: " + time 
            + ", timeout: " + timeout 
            + ", activeSearches: " + activeSearches;
    }
}
