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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.util.FixedSizeHashMap;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.Trie;
import com.limegroup.mojito.util.TrieUtils;
import com.limegroup.mojito.util.Trie.Cursor;

/**
 * 
 * 
 */
class BucketNode implements Bucket {

    public static enum Foo {
        LIVE,
        CACHE,
        BOTH;
    }
    
    private KUID bucketId;
    
    private int depth;
    
    private PatriciaTrie<KUID, Contact> nodeTrie;
    
    private Map<KUID, Contact> cache;
    
    private long timeStamp = 0L;
    
    public BucketNode(KUID bucketId, int depth) {
        this.bucketId = bucketId;
        this.depth = depth;
        
        nodeTrie = new PatriciaTrie<KUID, Contact>();
        
        cache = Collections.emptyMap();
    }
    
    public KUID getBucketID() {
        return bucketId;
    }
    
    public int getDepth() {
        return depth;
    }
    
    Trie<KUID, Contact> trie() {
        return nodeTrie;
    }
    
    public void touch() {
        timeStamp = System.currentTimeMillis();
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public void addLive(Contact node) {
        nodeTrie.put(node.getNodeID(), node);
    }
    
    public void addCache(Contact node) {
        if (cache == Collections.EMPTY_MAP) {
            int maxSize = RouteTableSettings.MAX_CACHE_SIZE.getValue();
            cache = new FixedSizeHashMap<KUID, Contact>(maxSize/2, 0.75f, true, maxSize);
        }
        
        cache.put(node.getNodeID(), node);
    }
    
    public Contact get(KUID nodeId) {
        Contact node = getLive(nodeId);
        if (node == null) {
            node = getCache(nodeId);
        }
        return node;
    }
    
    public Contact getLive(KUID nodeId) {
        return nodeTrie.get(nodeId);
    }
    
    public Contact getCache(KUID nodeId) {
        return cache.get(nodeId);
    }
    
    public Contact select(KUID nodeId) {
        return nodeTrie.select(nodeId);
    }
    
    public List<Contact> select(KUID nodeId, int count) {
        return TrieUtils.select(nodeTrie, nodeId, count);
    }
    
    public boolean remove(KUID nodeId) {
        if (removeLive(nodeId)) {
            return true;
        } else {
            return removeCache(nodeId);
        }
    }
    
    public boolean removeLive(KUID nodeId) {
        return nodeTrie.remove(nodeId) != null;
    }
    
    public boolean removeCache(KUID nodeId) {
        if (cache.remove(nodeId) != null) {
            if (cache.isEmpty()) {
                cache = Collections.emptyMap();
            }
            return true;
        } else {
            return false;
        }
    }
    
    public boolean contains(KUID nodeId) {
        if (containsLive(nodeId)) {
            return true;
        } else {
            return containsCache(nodeId);
        }
    }
    
    public boolean containsLive(KUID nodeId) {
        return nodeTrie.containsKey(nodeId);
    }
    
    public boolean containsCache(KUID nodeId) {
        return cache.containsKey(nodeId);
    }
    
    public boolean isLiveFull() {
        return nodeTrie.size() >= KademliaSettings.REPLICATION_PARAMETER.getValue();
    }
    
    public boolean isCacheFull() {
        return !cache.isEmpty() && ((FixedSizeHashMap<KUID, Contact>)cache).isFull();
    }
    
    public boolean isTooDeep() {
        return depth % RouteTableSettings.DEPTH_LIMIT.getValue() == 0;
    }
    
    public Collection<? extends Contact> live() {
        return nodeTrie.values();
    }
    
    public Collection<? extends Contact> cache() {
        return cache.values();
    }
    
    public Contact getLeastRecentlySeenLiveNode() {
        final Contact[] leastRecentlySeen = new Contact[]{ null };
        nodeTrie.traverse(new Cursor<KUID, Contact>() {
            public boolean select(Map.Entry<KUID, Contact> entry) {
                Contact node = entry.getValue();
                Contact lrs = leastRecentlySeen[0];
                if (lrs == null || node.getTimeStamp() < lrs.getTimeStamp()) {
                    leastRecentlySeen[0] = node;
                }
                return false;
            }
        });
        return leastRecentlySeen[0];
    }
    
    public Contact getMostRecentlySeenLiveNode() {
        final Contact[] mostRecentlySeen = new Contact[]{ null };
        nodeTrie.traverse(new Cursor<KUID, Contact>() {
            public boolean select(Map.Entry<KUID, Contact> entry) {
                Contact node = entry.getValue();
                Contact mrs = mostRecentlySeen[0];
                if (mrs == null || node.getTimeStamp() > mrs.getTimeStamp()) {
                    mostRecentlySeen[0] = node;
                }
                return false;
            }
        });
        return mostRecentlySeen[0];
    }

    // O(1)
    public Contact getLeastRecentlySeenCachedNode() {
        if (cache().isEmpty()) {
            return null;
        }
        
        return cache().iterator().next();
    }
    
    // O(n)
    public Contact getMostRecentlySeenCachedNode() {
        Contact node = null;
        for(Contact n : cache()) {
            node = n;
        }
        return node;
    }
    
    public List<? extends Bucket> split() {

        assert (cache().isEmpty() == true);
        
        BucketNode left = new BucketNode(bucketId, depth+1);
        BucketNode right = new BucketNode(bucketId.set(depth), depth+1);
        
        for (Contact node : live()) {
            KUID nodeId = node.getNodeID();
            if (!nodeId.isBitSet(depth)) {
                left.addLive(node);
            } else {
                right.addLive(node);
            }
        }
        
        /*for (Node node : cache()) {
            KUID nodeId = node.getNodeID();
            if (!nodeId.isBitSet(depth)) {
                left.addCache(node);
            } else {
                right.addCache(node);
            }
        }*/
        
        assert ((left.size() + right.size()) == size());
        return Arrays.asList(left, right);
    }
    
    public int size() {
        return getLiveSize() + getCacheSize();
    }
    
    public int getLiveSize() {
        return nodeTrie.size();
    }
    
    public int getLiveWithZeroFailures() {
        final int[] zeroFailures = new int[]{ 0 };
        nodeTrie.traverse(new Cursor<KUID, Contact>() {
            public boolean select(Map.Entry<KUID, Contact> entry) {
                Contact node = entry.getValue();
                if (!node.hasFailed()) {
                    zeroFailures[0]++;
                }
                return false;
            }
        });
        return zeroFailures[0];
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    public void clear() {
        nodeTrie.clear();
        cache = Collections.emptyMap();
    }
    
    public int hashCode() {
        return bucketId.hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof BucketNode)) {
            return false;
        }
        
        BucketNode other = (BucketNode)o;
        return bucketId.equals(other.bucketId)
                && depth == other.depth;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(bucketId).append(" (depth=").append(getDepth())
            .append(", live=").append(getLiveSize())
            .append(", cache=").append(getCacheSize()).append(")\n");
        
        Iterator<? extends Contact> it = live().iterator();
        for(int i = 0; it.hasNext(); i++) {
            buffer.append(" ").append(i).append(": ").append(it.next()).append("\n");
        }
        
        if (!cache().isEmpty()) {
            buffer.append("---\n");
            it = cache().iterator();
            for(int i = 0; it.hasNext(); i++) {
                buffer.append(" ").append(i).append(": ").append(it.next()).append("\n");
            }
        }
        
        return buffer.toString();
    }
}
