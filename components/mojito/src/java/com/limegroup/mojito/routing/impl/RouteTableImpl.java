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
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.exceptions.DHTException;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.statistics.RoutingStatisticContainer;
import com.limegroup.mojito.util.ContactUtils;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.Trie.Cursor;

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
     * A reference to the RouteTable callback
     */
    private transient Callback callback;
    
    public RouteTableImpl() {
        bucketTrie = new PatriciaTrie<KUID, Bucket>();
        init();
    }
    
    private void init() {
        KUID bucketId = KUID.MIN_NODE_ID;
        bucketTrie.put(bucketId, new BucketNode(this, bucketId, 0));
        
        consecutiveFailures = 0;
        smallestSubtreeBucket = null;
    }
    
    public void setRouteTableCallback(Callback callback) {
        if (routingStats == null && callback != null) {
            routingStats = new RoutingStatisticContainer(callback.getLocalNode().getNodeID());
        }
        this.callback = callback;
    }

    public synchronized void add(Contact node) {
        
        if (node.isFirewalled() && !isLocalNode(node)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(node + " is firewalled");
            }
            return;
        }
        
        consecutiveFailures = 0;
        
        KUID nodeId = node.getNodeID();
        Bucket bucket = bucketTrie.select(nodeId);
        Contact existing = bucket.get(nodeId);
        
        if (existing != null) {
            updateContactInBucket(bucket, existing, node);
        } else if (!bucket.isLiveFull()) {
            addContactToBucket(bucket, node);
        } else if (split(bucket)) {
            add(node); // re-try to add
        } else {
            replaceContactInBucket(bucket, node);
        }
    }
    
    protected synchronized void updateContactInBucket(Bucket bucket, Contact existing, Contact node) {
        assert (existing.getNodeID().equals(node.getNodeID()));
        
        /*
         * The other Node collides with our Node ID! Do nothing,
         * the other guy will change its Node ID!
         */
        if (isLocalNode(existing) && !isLocalNode(node)) {
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
            
        } else if (node.isAlive() 
                && !existing.hasBeenRecentlyAlive()) {
            
            doSpoofCheck(bucket, existing, node);
        }
    }
    
    protected synchronized void doSpoofCheck(Bucket bucket, final Contact existing, final Contact node) {
        PingListener listener = new PingListener() {
            public void handleResult(Contact result) {
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
                /*if (!(dhtEx.getCause() instanceof TimeoutException)) {
                    return;
                }*/
                
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
                        
                        node.updateWithExistingContact(current);
                        Contact replaced = bucket.updateContact(node);
                        assert (replaced == current);
                        
                        // If the Node is in the Cache then ping the least recently
                        // seen live Node which might promote the new Node to a
                        // live Contact!
                        if (bucket.containsCachedContact(nodeId)) {
                            ping (bucket.getLeastRecentlySeenLiveContact());
                        }
                    } else {
                        add(node);
                    }
                }
            }
        };
        
        ping(existing).addDHTEventListener(listener);
    }
    
    protected synchronized void addContactToBucket(Bucket bucket, Contact node) {
        bucket.addLiveContact(node);
        
        if (routingStats != null) {
            if (node.isAlive()) {
                routingStats.LIVE_NODE_COUNT.incrementStat();
            } else {
                routingStats.UNKNOWN_NODE_COUNT.incrementStat();
            }
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
                    throw new AssertionError("Neither left nor right Bucket contains the local Node");
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
            
            // WHOHOOO! WE SPLIT THE BUCKET!!!
            return true;
        }
        
        return false;
    }
    
    protected synchronized void replaceContactInBucket(Bucket bucket, Contact node) {
        
        if (node.isAlive()) {
            Contact leastRecentlySeen = bucket.getLeastRecentlySeenLiveContact();
            if (leastRecentlySeen.isUnknown() || leastRecentlySeen.isDead()) {
                if (LOG.isTraceEnabled()) {
                    LOG.info("Replacing " + leastRecentlySeen + " with " + node);
                }
                
                boolean  removed = bucket.removeLiveContact(leastRecentlySeen.getNodeID());
                assert (removed == true);
                
                bucket.addLiveContact(node);
                touchBucket(bucket);
                
                if (routingStats != null) {
                    routingStats.LIVE_NODE_COUNT.incrementStat();
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
            
            if (bucket.containsLiveContact(nodeId)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing " + node + " and replacing it with the MRS Node from Cache");
                }
                
                // Remove a live-dead Contact only if there's something 
                // in the replacement cache.
                
                Contact mrs = bucket.getMostRecentlySeenCachedContact();
                if (mrs != null) {
                    
                    boolean removed = bucket.removeCachedContact(mrs.getNodeID());
                    assert (removed == true);
                    
                    bucket.removeLiveContact(nodeId);
                    assert (bucket.isLiveFull() == false);
                    
                    bucket.addLiveContact(mrs);
                    
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
    
    public synchronized boolean isLocalBucket(KUID nodeId) {
        return bucketTrie.select(nodeId).contains(getLocalNode().getNodeID());
    }
    
    protected synchronized Bucket getBucket(KUID nodeId) {
        return bucketTrie.select(nodeId);
    }
    
    public synchronized Contact select(KUID nodeId) {
        return bucketTrie.select(nodeId).select(nodeId);
    }
    
    public synchronized Contact get(KUID nodeId) {
        return bucketTrie.select(nodeId).get(nodeId);
    }
    
    public synchronized List<Contact> select(final KUID nodeId, final int count) {
        final List<Contact> nodes = new ArrayList<Contact>(count);
        bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
            public boolean select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                nodes.addAll(bucket.select(nodeId, count - nodes.size()));
                return nodes.size() >= count;
            }
        });
        return nodes;
    }
    
    
    public synchronized List<Contact> select(final KUID nodeId, final int count, 
            final boolean liveContacts) {
        
        final List<Contact> nodes = new ArrayList<Contact>(count);
        
        if (liveContacts) {
            bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
                public boolean select(Entry<? extends KUID, ? extends Bucket> entry) {
                    Bucket bucket = entry.getValue();
                    for(Contact contact : bucket.select(nodeId, count - nodes.size())) {
                        if (!contact.hasFailed()) {
                            nodes.add(contact);
                        }
                    }
                    return nodes.size() >= count;
                }
            });
        } else {
            bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
                public boolean select(Entry<? extends KUID, ? extends Bucket> entry) {
                    Bucket bucket = entry.getValue();
                    nodes.addAll(bucket.select(nodeId, count - nodes.size()));
                    
                    return nodes.size() >= count;
                }
            });
        }
        return nodes;
    }
    
    public synchronized List<Contact> getContacts() {
        List<Contact> live = getLiveContacts();
        List<Contact> cached = getCachedContacts();
        
        List<Contact> nodes = new ArrayList<Contact>(live.size() + cached.size());
        nodes.addAll(live);
        nodes.addAll(cached);
        return nodes;
    }
    
    public synchronized List<Contact> getLiveContacts() {
        final List<Contact> nodes = new ArrayList<Contact>();
        bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public boolean select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                // This should be faster than addAll() as all  
                // elements are added straight to the 'nodes'
                // List but Cursors have on small sets an
                // overhead that does not pay off.
                //TrieUtils.values(bucket.trie(), nodes);
                nodes.addAll(bucket.getLiveContacts());
                return false;
            }
        });
        return nodes;
    }
    
    public synchronized List<Contact> getCachedContacts() {
        final List<Contact> nodes = new ArrayList<Contact>();
        bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public boolean select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                nodes.addAll(bucket.getCachedContacts());
                return false;
            }
        });
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
    public synchronized List<KUID> getRefreshIDs(final boolean bootstrapping) {
        final List<KUID> randomIds = new ArrayList<KUID>();
        
        bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public boolean select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                if (!bucket.contains(getLocalNode().getNodeID()) || !bootstrapping) {
                    if (bootstrapping || bucket.isRefreshRequired()) {
                        
                        // Select a random ID with this prefix
                        KUID randomId = KUID.createPrefxNodeID(bucket.getBucketID(), bucket.getDepth());
                        
                        if(LOG.isTraceEnabled()) {
                            LOG.trace("Refreshing bucket:" + bucket + " with random ID: " + randomId);
                        }
                        
                        randomIds.add(randomId);
                    }
                }
                return false;
            }
        });
        
        if (routingStats != null) {
            routingStats.BUCKET_REFRESH_COUNT.addData(randomIds.size());
        }
        
        return randomIds;
    }
    
    public synchronized List<Bucket> getBuckets() {
        return bucketTrie.values();
    }
    
    private void touchBucket(Bucket bucket) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Touching bucket: " + bucket);
        }
        
        bucket.touch();
    }
    
    private void pingLeastRecentlySeenNode(Bucket bucket) {
        ping(bucket.getLeastRecentlySeenLiveContact());
    }
    
    DHTFuture<Contact> ping(Contact node) {
        return callback.ping(node);
    }
    
    public Contact getLocalNode() {
        return callback.getLocalNode();
    }
    
    public boolean isLocalNode(Contact node) {
        return node.equals(getLocalNode());
    }
    
    public synchronized int size() {
        return getLiveContacts().size() + getCachedContacts().size();
    }
    
    public synchronized void clear() {
        bucketTrie.clear();
        init();
    }
    
    public synchronized void purge() {
    	bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public boolean select(Entry<? extends KUID, ? extends Bucket> entry) {
                Bucket bucket = entry.getValue();
                bucket.purge();
                return false;
            }
        });
    }
    
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Local: ").append(getLocalNode()).append("\n");
        
        for(Bucket bucket : getBuckets()) {
            buffer.append(bucket).append("\n");
        }
        
        buffer.append("Total Buckets: ").append(bucketTrie.size()).append("\n");
        buffer.append("Total Live Contacts: ").append(getLiveContacts().size()).append("\n");
        buffer.append("Total Cached Contacts: ").append(getCachedContacts().size()).append("\n");
        return buffer.toString();
    }
}