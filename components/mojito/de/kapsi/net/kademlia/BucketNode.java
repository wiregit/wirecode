package de.kapsi.net.kademlia;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.handler.DefaultMessageHandler;
import de.kapsi.net.kademlia.settings.RouteTableSettings;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;

public class BucketNode extends Node {
    
    private static final Log LOG = LogFactory.getLog(BucketNode.class);
    
    private int nodeCount;
    
    private int depth;
    
    private Cache replacementCache;

    public BucketNode(KUID nodeId,int depth) {
        super(nodeId);
        this.depth = depth;
        nodeCount = 0;
    }
    
    public void incrementNodeCount() {
        ++nodeCount;
    }
    
    public void decrementNodeCount() {
        --nodeCount;
    }

    public void setNodeCount(int count) {
        nodeCount = count;
    }
    
    public int getNodeCount() {
        return nodeCount;
    }
    
    public void addReplacementNode(ContactNode node) {
        //lazy instantiation of the replacement cache to save mem space
        if(replacementCache == null) {
            replacementCache = new Cache(RouteTableSettings.getMaxCacheSize());
        }
        replacementCache.put(node.getNodeID(), node);
    }
    
    public ContactNode getReplacementNode(KUID nodeId) {
        if(replacementCache == null) return null;
        else return (ContactNode)replacementCache.get(nodeId);
    }
    
    public int getReplacementCacheSize() {
        if(replacementCache == null) return 0;
        else return replacementCache.size();
    }
    
    public Map getReplacementCache() {
        return replacementCache;
    }
    
    public ContactNode getMostRecentlySeenCachedNode(boolean remove) {
        if(replacementCache == null) return null;
        else return (ContactNode)replacementCache.getMostRecentlySeen(remove);
    }
    
    public void removeReplacementNode(KUID nodeId) {
        if(replacementCache == null) return;
        else replacementCache.remove(nodeId);
    }
    
    public void touch() {
        super.alive();
    }
 
    public List split() {
        List list = new ArrayList();
        BucketNode leftBucket = new BucketNode(nodeId, depth+1);
        BucketNode rightBucket = new BucketNode(nodeId.set(depth, true),depth+1);
        if(replacementCache != null && !replacementCache.isEmpty()) {
            if(LOG.isErrorEnabled()) {
                LOG.error("Bucket node inconsistent: trying to split node with replacement cache not empty!");
            }
        }
        list.add(leftBucket);
        list.add(rightBucket);
        return list;
    }

    
    public int getDepth() {
        return depth;
    }
    
    public String toString() {
        return  super.toString() + ",depth: "+depth+", size: "+nodeCount+", replacements: "
        +(replacementCache==null?0:replacementCache.size());
    }
    
    public static void main(String[] args) {
        BucketNode bucket = new BucketNode(KUID.MIN_NODE_ID,0);
        System.out.println(bucket);
        for (int i = 0; i < 10; i++) {
            List l = bucket.split();
            BucketNode bucket0 = (BucketNode)l.get(0);
            bucket = (BucketNode)l.get(1);
            System.out.println(bucket0);
            System.out.println(bucket);
            System.out.println(KUID.createRandomID(bucket.getNodeID().getBytes(),bucket.getDepth()).toHexString());
        }
//        System.out.println(bucket2);
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof BucketNode)) {
            return false;
        }
        BucketNode other = (BucketNode)o;
        return (nodeId.equals((other.nodeId))) && (depth == other.depth);
    }
    
    /**
     * LRU replacement cache
     */
    private static class Cache extends FixedSizeHashMap {
        
        private Cache(int maxSize) {
            super(maxSize, 0.75f, true, maxSize);
        }
        
        // O(n)
        public Object getMostRecentlySeen(boolean remove) {
            if (isEmpty()) {
                return null;
            }
            
            Object value = null;
            Iterator it = values().iterator();
            while (it.hasNext()) {
                value = it.next();
            }
            
            if (remove && value != null) {
                it.remove();
            }
            return value;
        }
    }
}
