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

package com.limegroup.mojito.routing.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.statistics.RoutingStatisticContainer;
import com.limegroup.mojito.util.ContactUtils;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.Trie.Cursor;

public class RouteTableImpl implements RouteTable {
    
    private static final Log LOG = LogFactory.getLog(RouteTableImpl.class);
    
    protected final Context context;
    
    /**
     * The <tt>StatisticsContainer</tt> for the routing table stats.
     */
    private RoutingStatisticContainer routingStats;
    
    private PatriciaTrie<KUID, Bucket> bucketTrie;
    
    private int consecutiveFailures = 0;
    
    private Bucket smallestSubtreeBucket = null;
    
    public RouteTableImpl(Context context) {
        this.context = context;
        
        routingStats = new RoutingStatisticContainer(context);
        
        bucketTrie = new PatriciaTrie<KUID, Bucket>();
        init();
    }
    
    private void init() {
        KUID bucketId = KUID.MIN_NODE_ID;
        bucketTrie.put(bucketId, new BucketNode(context, bucketId, 0));
        
        consecutiveFailures = 0;
        smallestSubtreeBucket = null;
    }
    
    public synchronized void add(Contact node) {
        
        if (node.isFirewalled()) {
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
         * A non-live Contact will never replace a live Contact!
         */
        
        if (!existing.isAlive() 
                || context.isLocalNode(node)
                || existing.equals(node) // <- checks nodeId + address!
                || ContactUtils.areLocalContacts(existing, node)) {
            
            
            /*
             * See JIRA issue MOJITO-54
             */
            
            Contact contact = existing.mergeContacts(node);
            Contact replaced = bucket.updateContact(contact);
            assert (replaced == existing);
            
            if (contact.isAlive()) {
                touchBucket(bucket);
            }
            
        } else if (node.isAlive() 
                && !existing.hasBeenRecentlyAlive()) {
            
            doSpoofCheck(bucket, existing, node);
        }
    }
    
    protected synchronized void doSpoofCheck(Bucket bucket, final Contact existing, final Contact node) {
        PingListener listener = new PingListener() {
            public void response(ResponseMessage response, long time) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(node + " is trying to spoof " + response.getContact());
                }
                
                // DO NOTHING! The DefaultMessageHandler 
                // takes care of everything else!
            }

            public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
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
                        
                        Contact contact = current.mergeContacts(node);
                        Contact replaced = bucket.updateContact(contact);
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
        
        ping(existing, listener);
    }
    
    protected synchronized void addContactToBucket(Bucket bucket, Contact node) {
        bucket.addLiveContact(node);
        
        if (node.isAlive()) {
            routingStats.LIVE_NODE_COUNT.incrementStat();
        } else {
            routingStats.UNKNOWN_NODE_COUNT.incrementStat();
        }
    }
    
    protected synchronized boolean split(Bucket bucket) {
        
        // Three conditions for splitting:
        // 1. Bucket contains the local Node
        // 2. New node part of the smallest subtree to the local node
        // 3. current_depth mod symbol_size != 0
        
        boolean containsLocalNode = bucket.contains(context.getLocalNodeID());
        
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
                if (left.contains(context.getLocalNodeID())) {
                    smallestSubtreeBucket = right;
                } else if (right.contains(context.getLocalNodeID())) {
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
            routingStats.BUCKET_COUNT.incrementStat();
            
            // WHOHOOO! WE SPLIT THE BUCKET!!!
            return true;
        }
        
        return false;
    }
    
    protected synchronized void replaceContactInBucket(Bucket bucket, Contact node) {
        
        if (node.isAlive()) {
            Contact leastRecentlySeen = bucket.getLeastRecentlySeenLiveContact();
            if (leastRecentlySeen.isUnknown()) {
                if (LOG.isTraceEnabled()) {
                    LOG.info("Replacing " + leastRecentlySeen + " with " + node);
                }
                
                boolean  removed = bucket.removeLiveContact(leastRecentlySeen.getNodeID());
                assert (removed == true);
                
                bucket.addLiveContact(node);
                touchBucket(bucket);
                routingStats.LIVE_NODE_COUNT.incrementStat();
                return;
            }
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.info("Adding " + node + " to replacement cache");
        }
        
        // If the cache is full the least recently seen
        // node will be evicted!
        bucket.addCachedContact(node);
        routingStats.REPLACEMENT_COUNT.incrementStat();
        
        ping(bucket.getLeastRecentlySeenLiveContact());
    }
    
    public synchronized void handleFailure(KUID nodeId) {
        
        // NodeID might be null if we sent a ping to
        // an unknown Node (i.e. we knew only the
        // address) and the ping failed. 
        if (nodeId == null) {
            return;
        }
        
        // This should never happen -- who knows?!!
        if(context.isLocalNodeID(nodeId)) {
            return;
        }
        
        Bucket bucket = bucketTrie.select(nodeId);
        Contact node = bucket.get(nodeId);
        if (node == null) {
            // It's neither a live nor a cached Node
            // in the bucket!
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
            routingStats.DEAD_NODE_COUNT.incrementStat();
            
            if (bucket.containsLiveContact(nodeId)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing " + node + " and replacing it with the MRS Node from Cache");
                }
                
                bucket.removeLiveContact(nodeId);
                assert (bucket.isLiveFull() == false);
                
                Contact mrs = bucket.getMostRecentlySeenCachedContact();
                if (mrs != null) {
                    boolean removed = bucket.removeCachedContact(mrs.getNodeID());
                    assert (removed == true);
                    
                    mrs.unknown();
                    bucket.addLiveContact(mrs);
                    touchBucket(bucket);
                }
            } else {
                
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
    
    public synchronized boolean isCloseToLocal(KUID nodeId) {
        return bucketTrie.select(nodeId).contains(context.getLocalNodeID());
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
            public boolean select(Entry<KUID, Bucket> entry) {
                Bucket bucket = entry.getValue();
                nodes.addAll(bucket.select(nodeId, count-nodes.size()));
                return nodes.size() >= count;
            }
        });
        return nodes;
    }
    
    
    public synchronized List<Contact> select(final KUID nodeId, final int count, 
            final boolean liveContacts, final boolean willContact) {
        
        final List<Contact> nodes = new ArrayList<Contact>(count);
        
        if (liveContacts) {
            bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
                public boolean select(Entry<KUID, Bucket> entry) {
                    Bucket bucket = entry.getValue();
                    for(Contact contact : bucket.select(nodeId, count-nodes.size())) {
                        if (!contact.hasFailed()) {
                            nodes.add(contact);
                        }
                    }
                    
                    if (willContact) {
                        touchBucket(bucket);
                    }
                    
                    return nodes.size() >= count;
                }
            });
        } else {
            bucketTrie.select(nodeId, new Cursor<KUID, Bucket>() {
                public boolean select(Entry<KUID, Bucket> entry) {
                    Bucket bucket = entry.getValue();
                    nodes.addAll(bucket.select(nodeId, count-nodes.size()));
                    
                    if (willContact) {
                        touchBucket(bucket);
                    }
                    
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
            public boolean select(Entry<KUID, Bucket> entry) {
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
            public boolean select(Entry<KUID, Bucket> entry) {
                Bucket bucket = entry.getValue();
                nodes.addAll(bucket.getCachedContacts());
                return false;
            }
        });
        return nodes;
    }
    
    public synchronized List<KUID> getRefreshIDs(final boolean force) {
        final List<KUID> randomIds = new ArrayList<KUID>();
        
        bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public boolean select(Entry<KUID, Bucket> entry) {
                Bucket bucket = entry.getValue();
                if (!bucket.contains(context.getLocalNodeID())) {
                    if (force || bucket.isRefreshRequired()) {
                        
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
        
        routingStats.BUCKET_REFRESH_COUNT.addData(randomIds.size());
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
    
    private void ping(Contact node) {
        try {
            context.ping(node);
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    private void ping(Contact node, PingListener listener) {
        try {
            context.ping(node, listener);
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    public synchronized void clear() {
        bucketTrie.clear();
        init();
    }
    
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Local: ").append(context.getLocalNode()).append("\n");
        
        for(Bucket bucket : getBuckets()) {
            buffer.append(bucket).append("\n");
        }
        
        buffer.append("Total Buckets: ").append(bucketTrie.size()).append("\n");
        buffer.append("Total Live Contacts: ").append(getLiveContacts().size()).append("\n");
        buffer.append("Total Cached Contacts: ").append(getCachedContacts().size()).append("\n");
        return buffer.toString();
    }
}
