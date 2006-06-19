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
 
package com.limegroup.mojito;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.util.FixedSizeHashMap;

/**
 * A BucketNode is a virtual construct to simulate
 * Buckets on the PatriciaTrie.
 */
public class BucketNode extends Node {
    
    private static final long serialVersionUID = 2903713682317244655L;

    private static final Log LOG = LogFactory.getLog(BucketNode.class);
    
    /** The number of Contacts in this Bucket */
    private int nodeCount;
    
    /** The depth of this Bucket in the Trie */
    private int depth;
    
    private Cache replacementCache;
    
    public BucketNode(KUID nodeId, int depth) {
        super(nodeId);
        this.depth = depth;
        this.nodeCount = 0;
    }
    
    public void incrementNodeCount() {
        nodeCount++;
    }
    
    public void decrementNodeCount() {
        nodeCount--;
    }

    public void setNodeCount(int count) {
        nodeCount = count;
    }
    
    public int getNodeCount() {
        return nodeCount;
    }
    
    public void addReplacementNode(ContactNode node) {
        //lazy instantiation of the replacement cache to save mem space
        if (replacementCache == null) {
            replacementCache = new Cache(RouteTableSettings.MAX_CACHE_SIZE.getValue());
        }
        
        replacementCache.put(node.getNodeID(), node);
    }
    
    public ContactNode getReplacementNode(KUID nodeId) {
        return getReplacementCache().get(nodeId);
    }
    
    public int getReplacementCacheSize() {
        return getReplacementCache().size();
    }
    
    /**
     * Returns the replacement cache Map.
     * 
     * Do never ever try to modify this Map directly!
     * Use always the Iterator or the operators provided
     * by this class!
     */
    public Map<KUID, ContactNode> getReplacementCache() {
        if (replacementCache != null) {
            return replacementCache;
        }
        return Collections.EMPTY_MAP;
    }
    
    public ContactNode getMostRecentlySeenCachedNode(boolean remove) {
        if(replacementCache != null) {
            return replacementCache.getMostRecentlySeen(remove);
        }
        return null;
    }
    
    public ContactNode removeReplacementNode(KUID nodeId) {
        return getReplacementCache().remove(nodeId);
    }
    
    public void touch() {
        super.alive();
    }
 
    public List<BucketNode> split() {
        BucketNode leftBucket = new BucketNode(getNodeID(), depth+1);
        BucketNode rightBucket = new BucketNode(getNodeID().set(depth), depth+1);
        if (!getReplacementCache().isEmpty()) {
            if(LOG.isErrorEnabled()) {
                LOG.error("Bucket node inconsistent: trying to split node with replacement cache not empty!");
            }
        }
        return Arrays.asList(leftBucket, rightBucket);
    }

    public int getDepth() {
        return depth;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Bucket: " + getNodeID())
            .append(", depth: ").append(getDepth())
            .append(", size: ").append(getNodeCount())
            .append(", replacements: ").append(getReplacementCacheSize())
            .append(", timestamp: ").append(getTimeStamp());
        return buffer.toString();
    }
    
    /*public static void main(String[] args) {
        BucketNode bucket = new BucketNode(KUID.MIN_NODE_ID,0);
        System.out.println(bucket);
        for (int i = 0; i < 10; i++) {
            List l = bucket.split();
            BucketNode bucket0 = (BucketNode)l.get(0);
            bucket = (BucketNode)l.get(1);
            System.out.println(bucket0);
            System.out.println(bucket);
            System.out.println(KUID.createPrefxNodeID(bucket.getNodeID().getBytes(),bucket.getDepth()).toHexString());
        }
//        System.out.println(bucket2);
    }*/
    
    public boolean equals(Object o) {
        if (!(o instanceof BucketNode)) {
            return false;
        }
        
        return super.equals(o) 
            && (depth == ((BucketNode)o).depth);
    }
    
    /**
     * LRU replacement cache
     */
    private static class Cache extends FixedSizeHashMap<KUID, ContactNode> {
        
        private static final long serialVersionUID = 5255663117632404183L;

        private Cache(int maxSize) {
            super(maxSize, 0.75f, true, maxSize);
        }
        
        // O(1)
        public ContactNode getLeastRecentlySeen(boolean remove) {
            if (isEmpty()) {
                return null;
            }
            
            Iterator<ContactNode> it = values().iterator();
            ContactNode value = it.next();
            
            if (remove) {
                it.remove();
            }
            return value;
        }
        
        // O(n)
        public ContactNode getMostRecentlySeen(boolean remove) {
            if (isEmpty()) {
                return null;
            }

            ContactNode value = null;
            Iterator<ContactNode> it = values().iterator();
            while (it.hasNext()) {
                value = it.next();
            }
            
            if (remove) {
                it.remove();
            }
            return value;
        }
    }
}
