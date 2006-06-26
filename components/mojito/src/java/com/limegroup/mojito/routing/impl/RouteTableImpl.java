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
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.RouteTable;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.Trie.Cursor;

public class RouteTableImpl implements RouteTable {
    
    private static final Log LOG = LogFactory.getLog(RouteTableImpl.class);
    
    private Context context;
    
    private PatriciaTrie<KUID, Bucket> bucketTrie;
    
    private int consecutiveFailures = 0;
    
    private Bucket smallestSubtreeBucket = null;
    
    public RouteTableImpl(Context context) {
        this.context = context;
        
        bucketTrie = new PatriciaTrie<KUID, Bucket>();
        init();
    }
    
    private void init() {
        KUID bucketId = KUID.MIN_NODE_ID;
        bucketTrie.put(bucketId, new BucketNode(bucketId, 0));
        
        add(context.getLocalNode());
        
        consecutiveFailures = 0;
        smallestSubtreeBucket = null;
    }
    
    public void add(Contact node) {
        
        if (node.isFirewalled()) {
            if (LOG.isInfoEnabled()) {
                LOG.trace(node + " is firewalled");
            }
            return;
        }
        
        consecutiveFailures = 0;
        
        KUID nodeId = node.getNodeID();
        Bucket bucket = bucketTrie.select(nodeId);
        
        if (bucket.contains(nodeId)) {
            update(bucket, node);
        } else if (!bucket.isLiveFull()) {
            add(bucket, node);
        } else if (split(bucket)) {
            add(node);
        } else {
            replace(bucket, node);
        }
    }
    
    protected void update(Bucket bucket, Contact node) {
        Contact existing = bucket.get(node.getNodeID());
        assert (existing != null);
        
        if (!existing.isAlive() 
                || existing.equals(node)) {
            
            existing.set(node);
            if (node.isAlive()) {
                touchBucket(bucket);
            }
        } else if (node.isAlive() 
                && !existing.hasBeenRecentlyAlive()) {
            
            doSpoofCheck(bucket, existing, node);
        }
    }
    
    protected void doSpoofCheck(Bucket bucket, Contact existing, Contact node) {
        
    }
    
    protected void add(Bucket bucket, Contact node) {
        bucket.addLive(node);
    }
    
    protected boolean split(Bucket bucket) {
        
        // Three conditions for splitting:
        // 1. Bucket contains nodeID.
        // 2. New node part of the smallest subtree to the local node
        // 3. current_depth mod symbol_size != 0
        
        boolean containsLocalNode = bucket.contains(context.getLocalNodeID());
        
        if (containsLocalNode
                || bucket.equals(smallestSubtreeBucket)
                || !bucket.isTooDeep()) {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Splitting bucket: " + bucket);
            }
            
            List<? extends Bucket> buckets = bucket.split();
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
            
            // WHOHOOO! WE SPLIT THE BUCKET!!!
            return true;
        }
        
        return false;
    }
    
    protected void replace(Bucket bucket, Contact node) {
        
        if (node.isAlive()) {
            Contact leastRecentlySeen = bucket.getLeastRecentlySeenLiveNode();
            if (leastRecentlySeen.isUnknown()) {
                if (LOG.isTraceEnabled()) {
                    LOG.info("Replacing " + leastRecentlySeen + " with " + node);
                }
                
                boolean  removed = bucket.removeLive(leastRecentlySeen.getNodeID());
                assert (removed == true);
                
                bucket.addLive(node);
                touchBucket(bucket);
                return;
            }
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.info("Adding " + node + " to replacement cache");
        }
        
        // If the cache is full the least recently seen
        // node will be evicted!
        bucket.addCache(node);
        ping(bucket.getLeastRecentlySeenLiveNode());
    }
    
    public void handleFailure(KUID nodeId) {
        
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
            if (bucket.containsLive(nodeId)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing " + node + " and replacing it with the MRS Node from Cache");
                }
                
                bucket.removeLive(nodeId);
                assert (bucket.isLiveFull() == false);
                
                Contact mrs = bucket.getMostRecentlySeenCachedNode();
                if (mrs != null) {
                    boolean removed = bucket.removeCache(mrs.getNodeID());
                    assert (removed == true);
                    
                    mrs.unknown();
                    bucket.addLive(mrs);
                    touchBucket(bucket);
                }
            } else {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing " + node + " from Cache");
                }
                
                boolean removed = bucket.removeCache(nodeId);
                assert (removed == true);
            }
        }
    }
    
    protected boolean remove(Contact node) {
        return remove(node.getNodeID());
    }
    
    protected boolean remove(KUID nodeId) {
        return bucketTrie.select(nodeId).remove(nodeId);
    }
    
    public Contact select(KUID nodeId) {
        return bucketTrie.select(nodeId).select(nodeId);
    }
    
    public Contact get(KUID nodeId) {
        return bucketTrie.select(nodeId).get(nodeId);
    }
    
    public List<? extends Contact> select(final KUID nodeId, final int count) {
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
    
    
    public List<? extends Contact> select(final KUID nodeId, final int count, 
            final boolean liveNodes, final boolean willContact) {
        
        final List<Contact> nodes = new ArrayList<Contact>(count);
        
        if (liveNodes) {
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

    public List<? extends Contact> getNodes() {
        List<? extends Contact> live = getLiveNodes();
        List<? extends Contact> cached = getCachedNodes();
        
        List<Contact> nodes = new ArrayList<Contact>(live.size() + cached.size());
        nodes.addAll(live);
        nodes.addAll(cached);
        return nodes;
    }
    
    public List<? extends Contact> getLiveNodes() {
        final List<Contact> nodes = new ArrayList<Contact>();
        bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public boolean select(Entry<KUID, Bucket> entry) {
                Bucket bucket = entry.getValue();
                // This should be faster than addAll() as all  
                // elements are added straight to the 'nodes'
                // List but Cursors have on small sets an
                // overhead that does not pay off.
                //TrieUtils.values(bucket.trie(), nodes);
                nodes.addAll(bucket.live());
                return false;
            }
        });
        return nodes;
    }
    
    public List<? extends Contact> getCachedNodes() {
        final List<Contact> nodes = new ArrayList<Contact>();
        bucketTrie.traverse(new Cursor<KUID, Bucket>() {
            public boolean select(Entry<KUID, Bucket> entry) {
                Bucket bucket = entry.getValue();
                nodes.addAll(bucket.cache());
                return false;
            }
        });
        return nodes;
    }
    
    public List<? extends Bucket> getBuckets() {
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
    
    public void clear() {
        bucketTrie.clear();
        init();
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Buckets: ").append(bucketTrie.size()).append("\n");
        for(Bucket bucket : getBuckets()) {
            buffer.append(bucket).append("\n");
        }
        return buffer.toString();
    }
}
