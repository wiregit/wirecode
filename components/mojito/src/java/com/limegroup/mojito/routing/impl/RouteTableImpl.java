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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.PatriciaTrie;
import com.limegroup.gnutella.util.Trie.Cursor;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.DHTEventListener;
import com.limegroup.mojito.event.PingEvent;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.statistics.RoutingStatisticContainer;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.ContactUtils;

public class RouteTableImpl implements RouteTable {
    
    private static final long serialVersionUID = -7351267868357880369L;

    private static final Log LOG = LogFactory.getLog(RouteTableImpl.class);
    
    /**
     * The <tt>StatisticsContainer</tt> for the routing table stats.
     */
    private transient RoutingStatisticContainer routingStats;
    
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
     * A reference to the RouteTableCallback
     */
    private transient RouteTableCallback routeTableCallback;
    
    /**
     * The local Node
     */
    private Contact localNode;
    
    public RouteTableImpl() {
        bucketTrie = new PatriciaTrie<KUID, Bucket>(KUID.KEY_ANALYZER);
        init();
    }
    
    private void init() {
        KUID bucketId = KUID.MINIMUM;
        bucketTrie.put(bucketId, new BucketNode(this, bucketId, 0));
        
        consecutiveFailures = 0;
        smallestSubtreeBucket = null;
    }
    
    public void setPingCallback(PingCallback pingCallback) {
        this.pingCallback = pingCallback;
    }

    public void setRouteTableCallback(RouteTableCallback routeTableCallback) {
        this.routeTableCallback = routeTableCallback;
    }
    
    public synchronized void add(Contact node) {
        
        // The first Node we're adding to the RouteTable
        // is the local Node. Anyting else makes no sense
        // since all subsequent RouteTable operations
        // depend on the local Node.
        if (localNode == null) {
            if (!(node instanceof LocalContact)) {
                throw new IllegalArgumentException("The first Contact must be the local Contact: " + node);
            }
            
            this.localNode = node;
            routingStats = new RoutingStatisticContainer(localNode.getNodeID());
        }
        
        // Don't add firewalled Nodes except if it's the local Node
        if (node.isFirewalled() && !isLocalNode(node)) {
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
                
            // Must be of instance LocalContact!
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
                
                if (routeTableCallback != null) {
                    routeTableCallback.update(bucket, existing, node);
                }
            }
            
            return;
        }
        
        /*
         * A non-live Contact will never replace a live Contact!
         */
        if (existing.isAlive() && !node.isAlive() ) {
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
            if(bucket.containsCachedContact(node.getNodeID())
                    && (delay > RouteTableSettings.BUCKET_PING_LIMIT.getValue())) {
                pingLeastRecentlySeenNode(bucket);
            }
            touchBucket(bucket);
            
            if (routeTableCallback != null) {
                routeTableCallback.update(bucket, existing, node);
            }
            
        } else if (node.isAlive() 
                && !existing.hasBeenRecentlyAlive()) {
            
            doSpoofCheck(bucket, existing, node);
        }
    }
    
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
                
                if (!(ex instanceof DHTException)) {
                    return;
                }
                
                DHTException dhtEx = (DHTException)ex;
                KUID nodeId = dhtEx.getNodeID();
                SocketAddress address = dhtEx.getSocketAddress();
                
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
                        
                        if (routeTableCallback != null) {
                            routeTableCallback.update(bucket, current, node);
                        }
                        
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
        
        if (routeTableCallback != null) {
            routeTableCallback.check(bucket, existing, node);
        }
        
        ping(existing, listener);
        touchBucket(bucket);
    }
    
    protected synchronized void addContactToBucket(Bucket bucket, Contact node) {
        bucket.addActiveContact(node);
        
        if (routingStats != null) {
            if (node.isAlive()) {
                routingStats.LIVE_NODE_COUNT.incrementStat();
            } else {
                routingStats.UNKNOWN_NODE_COUNT.incrementStat();
            }
        }
        
        if (routeTableCallback != null) {
            routeTableCallback.add(bucket, node);
        }
    }
    
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
            
            // Increment by one 'cause see above.
            if (routingStats != null) {
                routingStats.BUCKET_COUNT.incrementStat();
            }
            
            if (routeTableCallback != null) {
                routeTableCallback.split(bucket, left, right);
            }
            
            // WHOHOOO! WE SPLIT THE BUCKET!!!
            return true;
        }
        
        return false;
    }
    
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
                
                if (routingStats != null) {
                    routingStats.LIVE_NODE_COUNT.incrementStat();
                }
                
                if (routeTableCallback != null) {
                    routeTableCallback.replace(bucket, leastRecentlySeen, node);
                }
                
                return;
            }
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.info("Adding " + node + " to replacement cache");
        }
        
        // If the cache is full the least recently seen
        // node will be evicted!
        bucket.addCachedContact(node);
        
        if (routingStats != null) {
            routingStats.REPLACEMENT_COUNT.incrementStat();
        }
        
        pingLeastRecentlySeenNode(bucket);
    }
    
    public synchronized void handleFailure(KUID nodeId, SocketAddress address) {
        
        // NodeID might be null if we sent a ping to
        // an unknown Node (i.e. we knew only the
        // address) and the ping failed. 
        if (nodeId == null) {
            return;
        }
        
        // This should never happen -- who knows?!!
        if(nodeId.equals(getLocalNode().getNodeID())) {
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
            
            if (routingStats != null) {
                routingStats.DEAD_NODE_COUNT.incrementStat();
            }
            
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
                    
                    if (routeTableCallback != null) {
                        routeTableCallback.replace(bucket, node, mrs);
                    }
                    
                } else if(node.getFailures() 
                            >= RouteTableSettings.MAX_ACCEPT_NODE_FAILURES.getValue()){
                    
                    bucket.removeActiveContact(nodeId);
                    assert (bucket.isActiveFull() == false);
                    
                    if (routeTableCallback != null) {
                        routeTableCallback.remove(bucket, node);
                    }
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
    
    protected synchronized boolean remove(Contact node) {
        return remove(node.getNodeID());
    }
    
    protected synchronized boolean remove(KUID nodeId) {
        return bucketTrie.select(nodeId).remove(nodeId);
    }
    
    public synchronized KUID getBucketID(KUID nodeId) {
        return bucketTrie.select(nodeId).getBucketID();
    }
    
    public synchronized Bucket getBucket(KUID nodeId) {
        return bucketTrie.select(nodeId);
    }
    
    public synchronized Contact select(KUID nodeId) {
        return bucketTrie.select(nodeId).select(nodeId);
    }
    
    public synchronized Contact get(KUID nodeId) {
        return bucketTrie.select(nodeId).get(nodeId);
    }
    
    public synchronized List<Contact> select(KUID nodeId, int count) {
        return select(nodeId, count, false);
    }
    
    public synchronized List<Contact> select(final KUID nodeId, final int count, 
            final boolean activeContacts) {
        
        final int maxNodeFailures = RouteTableSettings.MAX_ACCEPT_NODE_FAILURES.getValue();
        final List<Contact> nodes = new ArrayList<Contact>(count);
        bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
            public SelectStatus select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                List<Contact> list = bucket.select(nodeId, count - nodes.size());
                
                for(Contact contact : list) {
                    if (contact.isDead()) {
                        if (activeContacts) {
                            continue;
                        }
                        
                        float fact = (maxNodeFailures - contact.getFailures()) 
                                        / (float)Math.max(1, maxNodeFailures);
                        
                        if (Math.random() >= fact) {
                            continue;
                        }
                    }
                    
                    nodes.add(contact);
                }
                
                if (nodes.size() < count) {
                    return SelectStatus.CONTINUE;
                }
                return SelectStatus.EXIT;
            }
        });
        return nodes;
    }
    
    public synchronized List<Contact> getContacts() {
        List<Contact> live = getActiveContacts();
        List<Contact> cached = getCachedContacts();
        
        List<Contact> nodes = new ArrayList<Contact>(live.size() + cached.size());
        nodes.addAll(live);
        nodes.addAll(cached);
        return nodes;
    }
    
    public synchronized List<Contact> getActiveContacts() {
        List<Contact> nodes = new ArrayList<Contact>();
        for (Bucket bucket : bucketTrie.values()) {
            nodes.addAll(bucket.getActiveContacts());
        }
        return nodes;
    }
    
    public synchronized List<Contact> getCachedContacts() {
        List<Contact> nodes = new ArrayList<Contact>();
        for (Bucket bucket : bucketTrie.values()) {
            nodes.addAll(bucket.getCachedContacts());
        }
        return nodes;
    }
    
    /**
     * Returns a List of KUIDs that need to be looked up in order
     * to refresh the RouteTable.
     * 
     * If we are bootstrapping, we don't want to refresh the bucket
     * that contains the local node ID, as phase 1 already takes 
     * care of this. Additionally, when we bootstrap, we don't 
     * look at the bucket's timestamp (isRefreshRequired) so 
     * that we randomly fill up our routing table.
     * 
     * @param bootstrapping whether or not this refresh is done during bootstrap
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
        
        if (routingStats != null) {
            routingStats.BUCKET_REFRESH_COUNT.addData(randomIds.size());
        }
        
        return randomIds;
    }
    
    public synchronized Collection<Bucket> getBuckets() {
        return bucketTrie.values();
    }
    
    private void touchBucket(Bucket bucket) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Touching bucket: " + bucket);
        }
        
        bucket.touch();
    }
    
    private void pingLeastRecentlySeenNode(Bucket bucket) {
        Contact lrs = bucket.getLeastRecentlySeenActiveContact();
        if (!isLocalNode(lrs)) {
            ping(lrs, null);
        }
    }
    
    DHTFuture<PingEvent> ping(Contact node, DHTEventListener<PingEvent> listener) {
        DHTFuture<PingEvent> future = pingCallback.ping(node);
        if (listener != null) {
            future.addDHTEventListener(listener);
        }
        return future;
    }
    
    public Contact getLocalNode() {
        if (localNode == null) {
            throw new IllegalStateException("RouteTable is not initialized");
        }
        return localNode;
    }
    
    public boolean isLocalNode(Contact node) {
        return node.equals(getLocalNode());
    }
    
    public synchronized int size() {
        return getActiveContacts().size() + getCachedContacts().size();
    }
    
    public synchronized void clear() {
        bucketTrie.clear();
        localNode = null;
        routingStats = null;
        init();
        
        if (routeTableCallback != null) {
            routeTableCallback.clear();
        }
    }
    
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
        
        if (routeTableCallback != null) {
            routeTableCallback.clear();
        }
    }
    
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Local: ").append(getLocalNode()).append("\n");
        
        int alive = 0;
        int dead = 0;
        int unknown = 0;
        
        for(Bucket bucket : getBuckets()) {
            buffer.append(bucket).append("\n");
            
            for (Contact node : bucket.getActiveContacts()) {
                if (node.isAlive()) {
                    alive++;
                } else if (node.isDead()) {
                    dead++;
                } else {
                    unknown++;
                }
            }
            
            for (Contact node : bucket.getCachedContacts()) {
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
        buffer.append("Total Unknown Contacts: ").append(unknown).append("\n");
        return buffer.toString();
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        if (localNode != null) {
            routingStats = new RoutingStatisticContainer(localNode.getNodeID());
        }
    }
    
    /**
     * An interface to track various RouteTable operations. It's meant
     * for internal use only and we assume implementations don't throw
     * any exceptions and are non-blocking and super fast!
     */
    public static interface RouteTableCallback {
        
        /**
         * Called on Bucket splits
         * 
         * @param bucket The old Bucket
         * @param left The new left hand Bucket
         * @param right The new right hand Bucket
         */
        public void split(Bucket bucket, Bucket left, Bucket right);
        
        /**
         * Called when a new (active) Contact was added
         * 
         * @param bucket The Bucket where the new Node was added
         * @param node The new Node
         */
        public void add(Bucket bucket, Contact node);
        
        /**
         * Called when a Contact gets updated (both Contacts are the same
         * except for things like instance ID or RTT).
         * 
         * @param bucket The Bucket of the Contacts
         * @param existing The existing Contact
         * @param node The new Contact
         */
        public void update(Bucket bucket, Contact existing, Contact node);
        
        /**
         * Called when an existing Contact is being replaced by a different
         * Contact
         * 
         * @param bucket The Bucket of the Contacts
         * @param existing The existing Contact
         * @param node The new Contact
         */
        public void replace(Bucket bucket, Contact existing, Contact node);
        
        /**
         * Called when a Contact is removed from the RouteTable
         * 
         * @param bucket The Bucket of the Contact
         * @param node The Contact that is removed
         */
        public void remove(Bucket bucket, Contact node);
        
        /**
         * Called when the existing Contact collides with the other
         * Contact and a check is necessary
         * 
         * @param bucket The Bucket of the Contacts
         * @param existing The existing Contact
         * @param node The new Contact
         */
        public void check(Bucket bucket, Contact existing, Contact node);
        
        /**
         * Called when the route table is cleared or purged.
         */
        public void clear();
    }
}