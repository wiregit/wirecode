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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.util.FixedSizeHashMap;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.TrieUtils;
import com.limegroup.mojito.util.Trie.Cursor;

/**
 * 
 */
class BucketNode implements Bucket {
    
    private Context context;
    
    private KUID bucketId;
    
    private int depth;
    
    private PatriciaTrie<KUID, Contact> nodeTrie;
    
    private Map<KUID, Contact> cache;
    
    private long timeStamp = 0L;
    
    public BucketNode(Context context, KUID bucketId, int depth) {
        this.context = context;
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
    
    public void touch() {
        timeStamp = System.currentTimeMillis();
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public void addLiveContact(Contact node) {
        checkNodeID(node);
        assert (isLiveFull() == false);
        
        Contact existing = nodeTrie.put(node.getNodeID(), node);
        assert (existing == null);
        
        if(node.isAlive()) {
            touch();
        }
    }
    
    public void addCachedContact(Contact node) {
        checkNodeID(node);
        if (cache == Collections.EMPTY_MAP) {
            int maxSize = RouteTableSettings.MAX_CACHE_SIZE.getValue();
            cache = new FixedSizeHashMap<KUID, Contact>(maxSize/2, 0.75f, true, maxSize);
        }
        
        if (!isCacheFull()) {
            Contact existing = cache.put(node.getNodeID(), node);
            assert (existing == null);
        } else {
            Contact lrs = getLeastRecentlySeenCachedContact();
            if (!lrs.isAlive() || (!lrs.hasBeenRecentlyAlive() && node.isAlive())) {
                Contact c = cache.remove(lrs.getNodeID());
                assert (c == lrs);
                cache.put(node.getNodeID(), node);
            }
        }
    }
    
    public Contact updateContact(Contact node) {
        checkNodeID(node);
        
        KUID nodeId = node.getNodeID();
        if (containsLiveContact(nodeId)) {
            return nodeTrie.put(nodeId, node);
        } else if (containsCachedContact(nodeId)) {
            return cache.put(nodeId, node);
        }
        
        throw new IllegalStateException(node + " is not in this Bucket " + toString());
    }
    
    // TODO: Disable/Delete when finished with testing!
    private void checkNodeID(Contact node) {
        if (depth <= 0) {
            // This is the ROOT Bucket!
            return;
        }
        
        int bitIndex = bucketId.bitIndex(node.getNodeID());
        if (bitIndex < 0) {
            return;
        }
        
        assert (bitIndex >= depth) : "Wrong Bucket";
    }
    
    public Contact get(KUID nodeId) {
        Contact node = getLiveContact(nodeId);
        if (node == null) {
            node = getCachedContact(nodeId);
        }
        return node;
    }
    
    public Contact getLiveContact(KUID nodeId) {
        return nodeTrie.get(nodeId);
    }
    
    public Contact getCachedContact(KUID nodeId) {
        return cache.get(nodeId);
    }
    
    public Contact select(KUID nodeId) {
        return nodeTrie.select(nodeId);
    }
    
    public List<Contact> select(KUID nodeId, int count) {
        return TrieUtils.select(nodeTrie, nodeId, count);
    }
    
    public boolean remove(KUID nodeId) {
        if (removeLiveContact(nodeId)) {
            return true;
        } else {
            return removeCachedContact(nodeId);
        }
    }
    
    public boolean removeLiveContact(KUID nodeId) {
        return nodeTrie.remove(nodeId) != null;
    }
    
    public boolean removeCachedContact(KUID nodeId) {
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
        if (containsLiveContact(nodeId)) {
            return true;
        } else {
            return containsCachedContact(nodeId);
        }
    }
    
    public boolean containsLiveContact(KUID nodeId) {
        return nodeTrie.containsKey(nodeId);
    }
    
    public boolean containsCachedContact(KUID nodeId) {
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
    
    public Collection<Contact> getLiveContacts() {
        return nodeTrie.values();
    }
    
    public Collection<Contact> getCachedContacts() {
        return cache.values();
    }
    
    public Contact getLeastRecentlySeenLiveContact() {
        final Contact[] leastRecentlySeen = new Contact[]{ null };
        nodeTrie.traverse(new Cursor<KUID, Contact>() {
            public boolean select(Map.Entry<? extends KUID, ? extends Contact> entry) {
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
    
    public Contact getMostRecentlySeenLiveContact() {
        final Contact[] mostRecentlySeen = new Contact[]{ null };
        nodeTrie.traverse(new Cursor<KUID, Contact>() {
            public boolean select(Map.Entry<? extends KUID, ? extends Contact> entry) {
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
    public Contact getLeastRecentlySeenCachedContact() {
        if (getCachedContacts().isEmpty()) {
            return null;
        }
        
        return getCachedContacts().iterator().next();
    }
    
    // O(n)
    public Contact getMostRecentlySeenCachedContact() {
        Contact node = null;
        for(Contact n : getCachedContacts()) {
            node = n;
        }
        return node;
    }
    
    public List<Bucket> split() {

        assert (getCachedContacts().isEmpty() == true);
        
        Bucket left = new BucketNode(context, bucketId, depth+1);
        Bucket right = new BucketNode(context, bucketId.set(depth), depth+1);
        
        for (Contact node : getLiveContacts()) {
            KUID nodeId = node.getNodeID();
            if (!nodeId.isBitSet(depth)) {
                left.addLiveContact(node);
            } else {
                right.addLiveContact(node);
            }
        }
        
        assert ((left.size() + right.size()) == size());
        return Arrays.asList(left, right);
    }
    
    public int size() {
        return getLiveSize() + getCacheSize();
    }
    
    public int getLiveSize() {
        return nodeTrie.size();
    }
    
    private int getLiveNotDeadCount() {
        final int[] notDead = new int[]{ 0 };
        nodeTrie.traverse(new Cursor<KUID, Contact>() {
            public boolean select(Map.Entry<? extends KUID, ? extends Contact> entry) {
                Contact node = entry.getValue();
                if (!node.isDead()) {
                    notDead[0]++;
                }
                return false;
            }
        });
        return notDead[0];
    }
    
    public int getCacheSize() {
        return cache.size();
    }
    
    public boolean isRefreshRequired() {
        if ((System.currentTimeMillis() - getTimeStamp()) 
                >= RouteTableSettings.BUCKET_REFRESH_PERIOD.getValue()) {
            return true;
        }
        
        if (getLiveSize() < KademliaSettings.REPLICATION_PARAMETER.getValue()) {
            return true;
        }
        
        //we may have dead nodes in a bucket that has an empty replacement cache
        if (getLiveSize() != getLiveNotDeadCount()) {
            return true;
        }
        
        return false;
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
        
        Iterator<Contact> it = getLiveContacts().iterator();
        for(int i = 0; it.hasNext(); i++) {
            buffer.append(" ").append(i).append(": ").append(it.next()).append("\n");
        }
        
        if (!getCachedContacts().isEmpty()) {
            buffer.append("---\n");
            it = getCachedContacts().iterator();
            for(int i = 0; it.hasNext(); i++) {
                buffer.append(" ").append(i).append(": ").append(it.next()).append("\n");
            }
        }
        
        return buffer.toString();
    }
}
