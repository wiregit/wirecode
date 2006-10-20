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
 
package com.limegroup.mojito.routing.impl;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.PatriciaTrie;
import com.limegroup.gnutella.util.Trie.Cursor;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.event.DHTEventListener;
import com.limegroup.mojito.event.PingEvent;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.exceptions.DHTTimeoutException;
import com.limegroup.mojito.routing.Bucket;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.ContactFactory;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.routing.RouteTable.RouteTableEvent.EventType;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.ContactUtils;

/**
 * A PatriciaTrie bases RouteTable implementation for the Mojito DHT.
 * This is the reference implementation.
 */
public class RouteTableImpl implements RouteTable {
    
    private static final long serialVersionUID = -7351267868357880369L;

    private static final Log LOG = LogFactory.getLog(RouteTableImpl.class);
    
    /**
     * Trie of Buckets and the Buckets are a Trie of Contacts
     */
    private PatriciaTrie<KUID, Bucket> bucketTrie;
    
    /**
     * A counter for consecutive failures
     */
    private int consecutiveFailures = 0;
    
    /**
     * The smallest subtree Bucket
     */
    private Bucket smallestSubtreeBucket = null;
   
    /**
     * A reference to the PingCallback
     */
    private transient PingCallback pingCallback;
    
    /**
     * The local Node
     */
    private Contact localNode;
    
    /**
     * A list of RouteTableListeners. It's initialized lazily in
     * RouteTable#addRouteTableListener() 
     */
    private transient List<RouteTableListener> listeners;
    
    /**
     * Create a new RouteTable and generates a new random Node ID
     * for the local Node
     */
    public RouteTableImpl() {
        this(KUID.createRandomID());
    }
    
    /**
     * Create a new RouteTable and uses the given Node ID
     * for the local Node
     */
    public RouteTableImpl(byte[] nodeId) {
        this(KUID.create(nodeId));
    }
    
    /**
     * Create a new RouteTable and uses the given Node ID
     * for the local Node
     */
    public RouteTableImpl(String nodeId) {
        this(KUID.create(nodeId));
    }
    
    /**
     * Create a new RouteTable and uses the given Node ID
     * for the local Node
     */
    public RouteTableImpl(KUID nodeId) {
        localNode = ContactFactory.createLocalContact(0, 0, nodeId, 0, false);
        bucketTrie = new PatriciaTrie<KUID, Bucket>(KUID.KEY_ANALYZER);
        init();
    }
    
    /**
     * Initializes the RouteTable
     */
    private void init() {
        KUID bucketId = KUID.MINIMUM;
        Bucket bucket = new BucketNode(this, bucketId, 0);
        bucketTrie.put(bucketId, bucket);
        
        addContactToBucket(bucket, localNode);
        
        consecutiveFailures = 0;
        smallestSubtreeBucket = null;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#setPingCallback(com.limegroup.mojito.routing.RouteTable.PingCallback)
     */
    public void setPingCallback(PingCallback pingCallback) {
        this.pingCallback = pingCallback;
    }

    /**
     * Adds a RouteTableCallback
     * 
     * @param routeTableCallback The RouteTableCallback instance to add
     */
    public synchronized void addRouteTableListener(RouteTableListener l) {
        if (l == null) {
            throw new NullPointerException("RouteTableListener is null");
        }
        
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<RouteTableListener>();
        }
        
        listeners.add(l);
    }
    
    /**
     * Removes a RouteTableCallback
     * 
     * @param routeTableCallback The RouteTableCallback instance to remove
     */
    public synchronized void removeRouteTableListener(RouteTableListener l) {
        if (l == null) {
            throw new NullPointerException("RouteTableListener is null");
        }
        
        if (listeners != null) {
            listeners.remove(l);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#add(com.limegroup.mojito.Contact)
     */
    public synchronized void add(Contact node) {
        
        // Don't add firewalled Nodes
        if (node.isFirewalled()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(node + " is firewalled");
            }
            return;
        }
        
        // Make sure we're not mixing IPv4 and IPv6 addresses in the
        // RouteTable! IPv6 to IPv4 might work if there's a 6to4 gateway
        // or whatsoever but the opposite direction doesn't. An immediate
        // idea is to mark IPv6 Nodes as firewalled if they're contacting
        // IPv4 Nodes but this would lead to problems in the IPv6 network
        // if some IPv6 Nodes don't have access to a 6to4 gateway...
        if (!ContactUtils.isSameAddressSpace(localNode, node)) {
            
            // Log as ERROR so that we're not missing this
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " is from a different IP address space than " + localNode);
            }
            return;
        }
        
        consecutiveFailures = 0;
        
        KUID nodeId = node.getNodeID();
        Bucket bucket = bucketTrie.select(nodeId);
        Contact existing = bucket.get(nodeId);
        
        if (existing != null) {
            updateContactInBucket(bucket, existing, node);
        } else if (!bucket.isActiveFull()) {
            addContactToBucket(bucket, node);
        } else if (split(bucket)) {
            add(node); // re-try to add
        } else {
            replaceContactInBucket(bucket, node);
        }
    }
    
    /**
     * This method updates an existing Contact with data from a new Contact.
     * The initial state is that both Contacts have the same Node ID which
     * doesn't mean they're really the same Node. In order to figure out
     * if they're really equal it's peforming some additional checks and
     * there are a few side conditions.
     */
    protected synchronized void updateContactInBucket(Bucket bucket, Contact existing, Contact node) {
        assert (existing.getNodeID().equals(node.getNodeID()));
        
        if (isLocalNode(existing)) {
            // The other Node collides with our Node ID! Do nothing,
            // the other guy will change its Node ID! If it doesn't
            // everybody who has us in their RouteTable will ping us
            // to check if we're alive and we're hopefully able to
            // respond. Besides that there isn't much we can do. :-/
            if (!isLocalNode(node)) {
                if (LOG.isWarnEnabled()) {
                    LOG.debug(node + " collides with " + existing);
                }
                
            // Must be instance of LocalContact!
            } else if (!(node instanceof LocalContact)) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Attempting to replace the local Node " 
                            + existing + " with " + node);
                }
                
            // Alright, replace the existing Contact with the new
            // LocalContact. Log a warning... 
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Updating " + existing + " with " + node);
                }
                
                bucket.updateContact(node);
                this.localNode = node;
                
                fireContactUpdate(bucket, existing, node);
            }
            
            return;
        }
        
        /*
         * A non-live Contact will never replace a live Contact!
         */
        if (existing.isAlive() && !node.isAlive()) {
            return;
        }
        
        if (!existing.isAlive() 
                || isLocalNode(node)
                || existing.equals(node) // <- checks only nodeId + address!
                || ContactUtils.areLocalContacts(existing, node)) {
            
            /*
             * See JIRA issue MOJITO-54
             */
            
            node.updateWithExistingContact(existing);
            Contact replaced = bucket.updateContact(node);
            assert (replaced == existing);
            
            // a good time to ping least recently seen node if we know we
            // have a node alive in the replacement cache. Don't do this too often!
            long delay = System.currentTimeMillis() - bucket.getTimeStamp();
            if (bucket.containsCachedContact(node.getNodeID())
                    && (delay > RouteTableSettings.BUCKET_PING_LIMIT.getValue())) {
                pingLeastRecentlySeenNode(bucket);
            }
            touchBucket(bucket);
            
            fireContactUpdate(bucket, existing, node);
            
        } else if (node.isAlive() 
                && !existing.hasBeenRecentlyAlive()) {
            
            doSpoofCheck(bucket, existing, node);
        }
    }
    
    /**
     * This method tries to ping the existing Contact and if it doesn't
     * respond it will try to replace it with the new Contact. The initial 
     * state is that both Contacts have the same Node ID.
     */
    protected synchronized void doSpoofCheck(Bucket bucket, final Contact existing, final Contact node) {
        PingListener listener = new PingListener() {
            public void handleResult(PingEvent result) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(node + " is trying to spoof " + result);
                }
                
                // DO NOTHING! The DefaultMessageHandler takes care 
                // of everything else! DO NOT BAN THE NODE!!!
                // Reason: It was maybe just a Node ID collision!
            }
            
            public void handleThrowable(Throwable ex) {
                
                // We can only make decisions for timeouts! 
                if (!(ex instanceof DHTTimeoutException)) {
                    return;
                }
                
                DHTTimeoutException timeout = (DHTTimeoutException)ex;
                KUID nodeId = timeout.getNodeID();
                SocketAddress address = timeout.getSocketAddress();
                
                if (LOG.isInfoEnabled()) {
                    LOG.info(ContactUtils.toString(nodeId, address) 
                            + " did not respond! Replacing it with " + node);
                }
                
                synchronized (RouteTableImpl.this) {
                    Bucket bucket = bucketTrie.select(nodeId);
                    Contact current = bucket.get(nodeId);
                    if (current != null && current.equals(existing)) {
                        
                        /*
                         * See JIRA issue MOJITO-54
                         */
                        
                        // NOTE: We cannot call updateContactInBucket(...) here
                        // because it would do the spoof check again.
                        node.updateWithExistingContact(current);
                        Contact replaced = bucket.updateContact(node);
                        assert (replaced == current);
                        
                        fireContactUpdate(bucket, current, node);
                        
                        // If the Node is in the Cache then ping the least recently
                        // seen live Node which might promote the new Node to a
                        // live Contact!
                        if (bucket.containsCachedContact(nodeId)) {
                            pingLeastRecentlySeenNode(bucket);
                        }
                    } else {
                        add(node);
                    }
                }
            }
        };
        
        fireContactCheck(bucket, existing, node);
        
        ping(existing, listener);
        touchBucket(bucket);
    }
    
    /**
     * This method adds the given Contact to the given Bucket.
     */
    protected synchronized void addContactToBucket(Bucket bucket, Contact node) {
        bucket.addActiveContact(node);
        fireActiveContactAdded(bucket, node);
    }
    
    /**
     * This method splits the given Bucket into two new Buckets.
     * There are a few conditions in which cases we do split and
     * in which cases we don't.
     */
    protected synchronized boolean split(Bucket bucket) {
        
        // Three conditions for splitting:
        // 1. Bucket contains the local Node
        // 2. New node part of the smallest subtree to the local node
        // 3. current_depth mod symbol_size != 0
        
        boolean containsLocalNode = bucket.contains(getLocalNode().getNodeID());
        
        if (containsLocalNode
                || bucket.equals(smallestSubtreeBucket)
                || !bucket.isTooDeep()) {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Splitting bucket: " + bucket);
            }
            
            List<Bucket> buckets = bucket.split();
            assert (buckets.size() == 2);
            
            Bucket left = buckets.get(0);
            Bucket right = buckets.get(1);
            
            if (containsLocalNode) {
                if (left.contains(getLocalNode().getNodeID())) {
                    smallestSubtreeBucket = right;
                } else if (right.contains(getLocalNode().getNodeID())) {
                    smallestSubtreeBucket = left;
                } else {
                    throw new IllegalStateException("Neither left nor right Bucket contains the local Node");
                }
            }
            
            // The left one replaces the current bucket in the Trie!
            Bucket oldLeft = bucketTrie.put(left.getBucketID(), left);
            assert (oldLeft == bucket);
            
            // The right one is new in the Trie!
            Bucket oldRight = bucketTrie.put(right.getBucketID(), right);
            assert (oldRight == null);
            
            fireSplitBucket(bucket, left, right);
            
            // WHOHOOO! WE SPLIT THE BUCKET!!!
            return true;
        }
        
        return false;
    }
    
    /**
     * This method tries to replace an existing Contact in the given
     * Bucket with the given Contact or tries to add the given Contact
     * to the Bucket's replacement Cache. There are certain conditions
     * in which cases we replace Contacts and if it's not possible we're
     * trying to add the Contact to the replacement cache.
     */
    protected synchronized void replaceContactInBucket(Bucket bucket, Contact node) {
        
        if (node.isAlive()) {
            Contact leastRecentlySeen = bucket.getLeastRecentlySeenActiveContact();
            
            // If all Contacts in the given Bucket have the same time
            // stamp as the local Node then it's possible that the lrs
            // Contact is the local Contact in which case we don't want 
            // to replace the local Contact with the given Contact
            
            // Is the least recently seen node in UNKNOWN or DEAD state OR is the 
            // new Node a priority Node AND the lrs Node is NOT the local Node
            
            if (!isLocalNode(leastRecentlySeen) 
                    && (leastRecentlySeen.isUnknown() 
                            || leastRecentlySeen.isDead() 
                            || (node.getTimeStamp() == Contact.PRIORITY_CONTACT))) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.info("Replacing " + leastRecentlySeen + " with " + node);
                }
                
                boolean  removed = bucket.removeActiveContact(leastRecentlySeen.getNodeID());
                assert (removed == true);
                
                bucket.addActiveContact(node);
                touchBucket(bucket);
                
                fireReplaceContact(bucket, leastRecentlySeen, node);
                
                return;
            }
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.info("Adding " + node + " to replacement cache");
        }
        
        // If the cache is full the least recently seen
        // node will be evicted!
        Contact existing = bucket.addCachedContact(node);
        
        fireCachedContactAdded(bucket, existing, node);
        
        pingLeastRecentlySeenNode(bucket);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#handleFailure(com.limegroup.mojito.KUID, java.net.SocketAddress)
     */
    public synchronized void handleFailure(KUID nodeId, SocketAddress address) {
        
        // NodeID might be null if we sent a ping to
        // an unknown Node (i.e. we knew only the
        // address) and the ping failed. 
        if (nodeId == null) {
            return;
        }
        
        // This should never happen -- who knows?!!
        if(nodeId.equals(getLocalNode().getNodeID())) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot handle local Node's errors: " 
                        + ContactUtils.toString(nodeId, address));
            }
            return;
        }
        
        Bucket bucket = bucketTrie.select(nodeId);
        Contact node = bucket.get(nodeId);
        if (node == null) {
            // It's neither a live nor a cached Node
            // in the bucket!
            return;
        }
        
        if (!node.getContactAddress().equals(address)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(node + " address and " + address + " do not match");
            }
            return;
        }
        
        // Ignore failure if we start getting to many disconnections in a row
        if (consecutiveFailures 
                >= RouteTableSettings.MAX_CONSECUTIVE_FAILURES.getValue()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Ignoring node failure as it appears that we are disconnected");
            }
            return;
        }
        consecutiveFailures++;
        
        node.handleFailure();
        if (node.isDead()) {
            
            if (bucket.containsActiveContact(nodeId)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing " + node + " and replacing it with the MRS Node from Cache");
                }
                
                // Remove a live-dead Contact only if there's something 
                // in the replacement cache or if the node is over the limit.
                
                Contact mrs = bucket.getMostRecentlySeenCachedContact();
                if (mrs != null) {
                    
                    boolean removed = bucket.removeCachedContact(mrs.getNodeID());
                    assert (removed == true);
                    
                    bucket.removeActiveContact(nodeId);
                    assert (bucket.isActiveFull() == false);
                    
                    bucket.addActiveContact(mrs);
                    
                    fireReplaceContact(bucket, node, mrs);
                    
                } else if (node.getFailures() 
                            >= RouteTableSettings.MAX_ACCEPT_NODE_FAILURES.getValue()){
                    
                    bucket.removeActiveContact(nodeId);
                    assert (bucket.isActiveFull() == false);
                    
                    fireRemoveContact(bucket, node);
                }
            } else {
                
                // On first glance this might look like as if it is
                // not necessary since we're never contacting cached
                // Contacts but that's not absolutely true. FIND_NODE
                // lookups may return Contacts that are in our cache
                // and if they don't respond we want to remove them...
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing " + node + " from Cache");
                }
                
                boolean removed = bucket.removeCachedContact(nodeId);
                assert (removed == true);
            }
        }
    }
    
    /**
     * Removes the given Contact from the RouteTable
     */
    protected synchronized boolean remove(Contact node) {
        return remove(node.getNodeID());
    }
    
    /**
     * Removes the given KUID (Contact with that KUID) 
     * from the RouteTable
     */
    protected synchronized boolean remove(KUID nodeId) {
        return bucketTrie.select(nodeId).remove(nodeId);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getBucketID(com.limegroup.mojito.KUID)
     */
    public synchronized KUID getBucketID(KUID nodeId) {
        return bucketTrie.select(nodeId).getBucketID();
    }
    
    /**
     * Returns a Bucket that is nearest (xor distance) 
     * to the given KUID
     */
    public synchronized Bucket getBucket(KUID nodeId) {
        return bucketTrie.select(nodeId);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#select(com.limegroup.mojito.KUID)
     */
    public synchronized Contact select(KUID nodeId) {
        return bucketTrie.select(nodeId).select(nodeId);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#get(com.limegroup.mojito.KUID)
     */
    public synchronized Contact get(KUID nodeId) {
        return bucketTrie.select(nodeId).get(nodeId);
    }
    
    /**
     * Returns 'count' number of Contacts that are nearest (xor distance)
     * to the given KUID.
     */
    public synchronized List<Contact> select(KUID nodeId, int count) {
        return select(nodeId, count, false);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#select(com.limegroup.mojito.KUID, int, boolean)
     */
    public synchronized List<Contact> select(final KUID nodeId, final int count, 
            final boolean activeContacts) {
        
        final int maxNodeFailures = RouteTableSettings.MAX_ACCEPT_NODE_FAILURES.getValue();
        final List<Contact> nodes = new ArrayList<Contact>(count);
        bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                
                Collection<Contact> list = null;
                if (activeContacts) {
                    // Select all Contacts from the Bucket to compensate
                    // the fact that not all of them will be alive. We're
                    // using Bucket.select() instead of Bucket.getActiveContacts()
                    // to get the Contacts sorted by xor distance!
                    list = bucket.select(nodeId, bucket.getActiveSize());
                } else {
                    list = bucket.select(nodeId, count);
                }
                
                for(Contact node : list) {
                    
                    // Ignore all non-alive Contacts if only
                    // active Contacts are requested.
                    // TODO: See LocalContact.isAlive() !!! 
                    if (activeContacts && !node.isAlive()) {
                        continue;
                    }
                    
                    // Ignore all Contacts that are down
                    if (node.isShutdown()) {
                        continue;
                    }
                    
                    if (node.isDead()) {
                        float fact = (maxNodeFailures - node.getFailures()) 
                                        / (float)Math.max(1, maxNodeFailures);
                        
                        if (Math.random() >= fact) {
                            continue;
                        }
                    }
                    
                    nodes.add(node);
                    
                    // Exit the loop if done
                    if (nodes.size() >= count) {
                        return SelectStatus.EXIT;
                    }
                }
                
                return SelectStatus.CONTINUE;
            }
        });
        
        assert (nodes.size() <= count);
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getContacts()
     */
    public synchronized List<Contact> getContacts() {
        List<Contact> live = getActiveContacts();
        List<Contact> cached = getCachedContacts();
        
        List<Contact> nodes = new ArrayList<Contact>(live.size() + cached.size());
        nodes.addAll(live);
        nodes.addAll(cached);
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getActiveContacts()
     */
    public synchronized List<Contact> getActiveContacts() {
        List<Contact> nodes = new ArrayList<Contact>();
        for (Bucket bucket : bucketTrie.values()) {
            nodes.addAll(bucket.getActiveContacts());
        }
        return nodes;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getCachedContacts()
     */
    public synchronized List<Contact> getCachedContacts() {
        List<Contact> nodes = new ArrayList<Contact>();
        for (Bucket bucket : bucketTrie.values()) {
            nodes.addAll(bucket.getCachedContacts());
        }
        return nodes;
    }
    
    /*
     * If we are bootstrapping, we don't want to refresh the bucket
     * that contains the local node ID, as phase 1 already takes 
     * care of this. Additionally, when we bootstrap, we don't 
     * look at the bucket's timestamp (isRefreshRequired) so 
     * that we randomly fill up our routing table.
     * 
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getRefreshIDs(boolean)
     */
    public synchronized List<KUID> getRefreshIDs(boolean bootstrapping) {
        List<KUID> randomIds = new ArrayList<KUID>();
        for (Bucket bucket : bucketTrie.values()) {
            
            if (bootstrapping 
                    && bucket.contains(getLocalNode().getNodeID())) {
                // Don't refresh the local Bucket if we're bootstrapping
                // since phase one takes already care of it.
                continue;
            }
            
            if (bootstrapping || bucket.isRefreshRequired()) {
                
                // Select a random ID with this prefix
                KUID randomId = KUID.createPrefxNodeID(
                        bucket.getBucketID(), bucket.getDepth());
                
                if(LOG.isTraceEnabled()) {
                    LOG.trace("Refreshing bucket:" + bucket 
                            + " with random ID: " + randomId);
                }
                
                randomIds.add(randomId);
                touchBucket(bucket);
            }
        }
        
        return randomIds;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getBuckets()
     */
    public synchronized Collection<Bucket> getBuckets() {
        return Collections.unmodifiableCollection(bucketTrie.values());
    }
    
    /**
     * Touches the given Bucket (i.e. updates its timeStamp)
     */
    private void touchBucket(Bucket bucket) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Touching bucket: " + bucket);
        }
        
        bucket.touch();
    }
    
    /**
     * Pings the least recently seen active Contact in the given Bucket
     */
    private void pingLeastRecentlySeenNode(Bucket bucket) {
        Contact lrs = bucket.getLeastRecentlySeenActiveContact();
        if (!isLocalNode(lrs)) {
            ping(lrs, null);
        }
    }
    
    /**
     * Pings the given Contact and adds the given DHTEventListener to
     * the DHTFuture if it's not null
     */
    DHTFuture<PingEvent> ping(Contact node, DHTEventListener<PingEvent> listener) {
        DHTFuture<PingEvent> future = null;
        
        if (pingCallback != null) {
            future = pingCallback.ping(node);
            
        } else {
            future = new DefaultDHTFuture(node);
            handleFailure(node.getNodeID(), node.getContactAddress());
        }
        
        if (listener != null) {
            future.addDHTEventListener(listener);
        }
        
        return future;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#getLocalNode()
     */
    public Contact getLocalNode() {
        if (localNode == null) {
            throw new IllegalStateException("RouteTable is not initialized");
        }
        return localNode;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#isLocalNode(com.limegroup.mojito.Contact)
     */
    public boolean isLocalNode(Contact node) {
        return node.equals(getLocalNode());
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#size()
     */
    public synchronized int size() {
        return getActiveContacts().size() + getCachedContacts().size();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#clear()
     */
    public synchronized void clear() {
        bucketTrie.clear();
        fireClear();
        init();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#purge()
     */
    public synchronized void purge() {
        if (localNode == null) {
            throw new IllegalStateException("RouteTable is not initialized");
        }
        
        bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                bucket.purge();
                return SelectStatus.CONTINUE;
            }
        });
        
        // Rebuild the RouteTable (merge Buckets) but don't do a 
        // true rebuild. That means forget the cached Contacts! 
        // This is more about merging Buckets than actually rebuilding
        // the RouteTable.
        rebuild(false);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.routing.RouteTable#rebuild()
     */
    public synchronized void rebuild() {
        if (localNode == null) {
            throw new IllegalStateException("RouteTable is not initialized");
        }
        
        // Do a true RouteTable rebuild.
        rebuild(true);
    }
    
    /**
     * Gets copies of the current RouteTable, clears the
     * current RouteTable and re-adds the Contacts from
     * the copies.
     */
    private void rebuild(boolean isTrueRebuild) {
        
        // Get the local Node (clear() will set it to null)
        Contact localNode = this.localNode;
        
        // Get the active Contacts
        List<Contact> activeNodes = getActiveContacts();
        activeNodes = BucketUtils.sortAliveToFailed(activeNodes);
        
        // Get the cached Contacts
        List<Contact> cachedNodes = null;
        if (isTrueRebuild) {
            cachedNodes = getCachedContacts();
            cachedNodes = BucketUtils.sort(cachedNodes);
        }
        
        // We count on the fact that getActiveContacts() and 
        // getCachedContacts() return copies!
        clear();
        
        // Remove the local Node from the List. Shouldn't fail as 
        // activeNodes is a copy!
        boolean removed = activeNodes.remove(localNode);
        assert (removed);
        
        // Add the local Node first!
        add(localNode);
        
        // Re-add the active Contacts
        for (Contact node : activeNodes) {
            if (isTrueRebuild) {
                node.unknown();
            }
            
            add(node);
        }
        
        // And re-add the cached Contacts
        if (isTrueRebuild) {
            for (Contact node : cachedNodes) {
                node.unknown();
                add(node);
            }
        }
    }
    
    private void fireActiveContactAdded(Bucket bucket, Contact node) {
        fireRouteTableEvent(bucket, null, null, null, node, EventType.ADD_ACTIVE_CONTACT);
    }
    
    private void fireCachedContactAdded(Bucket bucket, Contact existing, Contact node) {
        fireRouteTableEvent(bucket, null, null, existing, node, EventType.ADD_CACHED_CONTACT);
    }
    
    private void fireContactUpdate(Bucket bucket, Contact existing, Contact node) {
        fireRouteTableEvent(bucket, null, null, existing, node, EventType.UPDATE_CONTACT);
    }
    
    private void fireReplaceContact(Bucket bucket, Contact existing, Contact node) {
        fireRouteTableEvent(bucket, null, null, existing, node, EventType.REPLACE_CONTACT);
    }
    
    private void fireRemoveContact(Bucket bucket, Contact node) {
        fireRouteTableEvent(bucket, null, null, null, node, EventType.REMOVE_CONTACT);
    }
    
    private void fireContactCheck(Bucket bucket, Contact existing, Contact node) {
        fireRouteTableEvent(bucket, null, null, existing, node, EventType.CONTACT_CHECK);
    }
    
    private void fireSplitBucket(Bucket bucket, Bucket left, Bucket right) {
        fireRouteTableEvent(bucket, left, right, null, null, EventType.SPLIT_BUCKET);
    }
    
    private void fireClear() {
        fireRouteTableEvent(null, null, null, null, null, EventType.CLEAR);
    }
    
    private void fireRouteTableEvent(Bucket bucket, Bucket left, Bucket right, 
            Contact existing, Contact node, EventType type) {
        
        // To keep the overhead low we're waiting till last
        // minute with creating the RouteTableEvent object.
        
        List<RouteTableListener> l = listeners;
        if (l != null) {
            Iterator<RouteTableListener> it = l.iterator();
            if (it.hasNext()) {
                
                // OK, we know now that there's at least one listener!
                // Create the RouteTableEvent object!
                RouteTableEvent event = new RouteTableEvent(
                        this, bucket, left, right, existing, node, type);
                
                // And fire the Events!
                while(it.hasNext()) {
                    it.next().handleRouteTableEvent(event);
                }
            }
        }
    }
    
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Local: ").append(getLocalNode()).append("\n");
        
        int alive = 0;
        int dead = 0;
        int down = 0;
        int unknown = 0;
        
        for(Bucket bucket : getBuckets()) {
            buffer.append(bucket).append("\n");
            
            for (Contact node : bucket.getActiveContacts()) {
                if (node.isShutdown()) {
                    down++;
                }
                
                if (node.isAlive()) {
                    alive++;
                } else if (node.isDead()) {
                    dead++;
                } else {
                    unknown++;
                }
            }
            
            for (Contact node : bucket.getCachedContacts()) {
                if (node.isShutdown()) {
                    down++;
                }
                
                if (node.isAlive()) {
                    alive++;
                } else if (node.isDead()) {
                    dead++;
                } else {
                    unknown++;
                }
            }
        }
        
        buffer.append("Total Buckets: ").append(bucketTrie.size()).append("\n");
        buffer.append("Total Active Contacts: ").append(getActiveContacts().size()).append("\n");
        buffer.append("Total Cached Contacts: ").append(getCachedContacts().size()).append("\n");
        buffer.append("Total Alive Contacts: ").append(alive).append("\n");
        buffer.append("Total Dead Contacts: ").append(dead).append("\n");
        buffer.append("Total Down Contacts: ").append(down).append("\n");
        buffer.append("Total Unknown Contacts: ").append(unknown).append("\n");
        return buffer.toString();
    }
    
    /**
     * A dummy implementation of DHTFuture that emulates a ping timeout
     */
    private static class DefaultDHTFuture implements DHTFuture<PingEvent> {
        
        private Contact node;
        
        public DefaultDHTFuture(Contact node) {
            this.node = node;
        }
        
        public void addDHTEventListener(DHTEventListener<PingEvent> listener) {
            listener.handleThrowable(new DHTTimeoutException(
                    node.getNodeID(), node.getContactAddress(), null, 0L));
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public PingEvent get() throws InterruptedException, ExecutionException {
            throw new ExecutionException(
                    new DHTTimeoutException(
                            node.getNodeID(), node.getContactAddress(), null, 0L));
        }

        public PingEvent get(long timeout, TimeUnit unit) 
                throws InterruptedException, ExecutionException, TimeoutException {
            throw new ExecutionException(
                    new DHTTimeoutException(
                            node.getNodeID(), node.getContactAddress(), null, 0L));
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return true;
        }
    }
}