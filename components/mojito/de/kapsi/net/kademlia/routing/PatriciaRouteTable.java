package de.kapsi.net.kademlia.routing;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.BucketNode;
import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.RouteTableSettings;
import de.kapsi.net.kademlia.util.BucketUtils;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class PatriciaRouteTable implements RoutingTable{
    
    private static final Log LOG = LogFactory.getLog(PatriciaRouteTable.class);
    
    private static final int K = KademliaSettings.getReplicationParameter();
    
    private static final int B = RouteTableSettings.getDepthLimit();
    
    private static final long refreshLimit = RouteTableSettings.getBucketRefreshTime();
    
    private final Context context;
    
    private final PatriciaTrie nodesTrie;
    
    private final PatriciaTrie bucketsTrie;
    
    private final HashMap staleNodes;

    public PatriciaRouteTable(Context context) {
        this.context = context;
        
        staleNodes = new HashMap();
        
        nodesTrie = new PatriciaTrie();
        bucketsTrie = new PatriciaTrie();
        
        
        KUID rootKUID = KUID.MIN_NODE_ID;
        BucketNode root = new BucketNode(rootKUID,0);
        bucketsTrie.put(rootKUID,root);
    }

    public void add(ContactNode node, boolean alive) {
        put(node.getNodeID(), node,alive);
    }
    
    private void put(KUID nodeId, ContactNode node, boolean alive) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Trying to add node: "+node+" to routing table");
        }
        
        if (nodeId == null) {
            throw new IllegalArgumentException("NodeID is null");
        }
        
        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }
        
        if (!nodeId.equals(node.getNodeID())) {
            throw new IllegalArgumentException("NodeID and the ID returned by Node do not match");
        }
        
        staleNodes.remove(nodeId);
        if(updateExistingNode(nodeId,node,alive)) {
            return;
        }
        //get bucket closest to node
        BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
        if(bucket.getNodeCount() < K) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Adding node: "+node+" to bucket: "+bucket);
            }
            bucket.incrementNodeCount();
            bucket.removeReplacementNode(nodeId);
            nodesTrie.put(nodeId,node);
            return;
        } 
        //Three conditions for splitting:
        //1. Bucket contains nodeID.
        //2. New node part of the smallest subtree to the local node
        //2. current_depth mod symbol_size != 0
        else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Bucket "+bucket+" full");
            }
            
            BucketNode localBucket = (BucketNode)bucketsTrie.select(context.getLocalNodeID());
            //1
            boolean containsLocal = localBucket.equals(bucket);
            //2
            BucketNode smallestSubtree = (BucketNode)bucketsTrie.selectNextClosest(localBucket.getNodeID());
            
            boolean partOfSmallest = bucket.equals(smallestSubtree);
            //3
            boolean tooDeep = bucket.getDepth() % B == 0;
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Bucket "+bucket+" full:" +
                        "\ncontainsLocal: " + containsLocal + 
                        "\npartOfSmallest: " + partOfSmallest + 
                        "\nNot tooDeep: "+!tooDeep);
            }
            
            if(containsLocal || partOfSmallest || !tooDeep) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("splitting bucket: " + bucket);
                }
                
                List newBuckets = bucket.split();
                //update bucket node count
                BucketNode leftSplitBucket = (BucketNode) newBuckets.get(0);
                BucketNode rightSplitBucket = (BucketNode) newBuckets.get(1);
                bucketsTrie.put(leftSplitBucket.getNodeID(),leftSplitBucket);
                bucketsTrie.put(rightSplitBucket.getNodeID(),rightSplitBucket);
                int countLeft = updateBucketNodeCount(leftSplitBucket);
                int countRight = updateBucketNodeCount(rightSplitBucket);
                //this should never happen
                if(countLeft+countRight != bucket.getNodeCount()) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Bucket did not split correctly!");
                    }
                    return;
                }
                //trying recursive call!
                //attempt the put the new contact again with the split buckets
                put(nodeId,node,alive);
                
            } 
            //not splitting --> replacement cache
            else {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("NOT splitting bucket "+ bucket+", adding node "+node+" to replacement cache");
                }
                
                addReplacementNode(bucket,node);
            }
        }
    }
    
    public void handleFailure(KUID nodeId) {
        
        //this should never happen -- who knows?!!
        if(nodeId.equals(context.getLocalNodeID())) {
            if(LOG.isErrorEnabled()) {
                LOG.error("Local node marked as dead!");
            }
        }
        ContactNode node = (ContactNode) nodesTrie.get(nodeId);
        //get closest bucket
        BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
        Map replacementCache = bucket.getReplacementCache();
        if(node != null) {
            if(node.failure() > RouteTableSettings.getMaxNodeFailures()) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing node: "+node+" from bucket: "+bucket);
                }
                //remove node and replace with most recent alive one from cache
                nodesTrie.remove(nodeId);
                bucket.decrementNodeCount();
                if(replacementCache != null && replacementCache.size()!=0) {
                    ContactNode replacement = bucket.getMostRecentlySeenCachedNode(true);
                    put(replacement.getNodeID(),replacement,false);
                }
            }
        } else {
            if(replacementCache!= null) {
                node = (ContactNode)replacementCache.remove(nodeId);
                if (node!= null && LOG.isTraceEnabled()) {
                    LOG.trace("Removed node: "+node+" from replacement cache");
                }
            }
        }
    }
    
    public int updateBucketNodeCount(BucketNode bucket) {
        int newCount = nodesTrie.range(bucket.getNodeID(),bucket.getDepth()-1).size();
        bucket.setNodeCount(newCount);
        return newCount;
    }
    
    /**
     * Adds a node to the replacement cache of the corresponding bucket
     * 
     * @param bucket
     * @param node
     */
    public void addReplacementNode(BucketNode bucket,ContactNode node) {
        
        boolean added = false;
        
        Map replacementCache = bucket.getReplacementCache();

        if(replacementCache!= null &&
                replacementCache.size() == RouteTableSettings.getMaxCacheSize()) {
            //replace older cache entries with this one
            for (Iterator iter = replacementCache.values().iterator(); iter.hasNext();) {
                ContactNode oldNode = (ContactNode) iter.next();
                
                if(oldNode.getTimeStamp() <= node.getTimeStamp()) {
                    replacementCache.remove(oldNode);
                    replacementCache.put(node.getNodeID(),node);
                    added = true;
                }
            }
        } else {
            bucket.addReplacementNode(node);
            added = true;
        }
        //ping least recently seen node
        if(added) {
            List bucketList = nodesTrie.range(bucket.getNodeID(),bucket.getDepth()-1);
            ContactNode leastRecentlySeen = 
                BucketUtils.getLeastRecentlySeen(BucketUtils.sort(bucketList));

            if (LOG.isTraceEnabled()) {
                LOG.trace("Pinging the least recently seen Node " 
                        + leastRecentlySeen);
            }
            
            PingRequest ping = context.getMessageFactory().createPingRequest();
            
            //TODO fix pinging
            //context.getMessageDispatcher().send(leastRecentlySeen, ping, this);
        }
    }
    
    
    /**
     * Checks the local table and replacement nodes and updates timestamp.
     * 
     * 
     * @param nodeId The contact nodeId
     * @param node The contact node 
     * @param alive If the contact is alive
     * 
     * @return true if the contact exists and has been updated, false otherwise
     */
    public boolean updateExistingNode(KUID nodeId, ContactNode node, boolean alive) {
        boolean replacement = false;
        ContactNode existingNode = (ContactNode) nodesTrie.get(nodeId);
        if(existingNode == null) {
            //check replacement cache in closest bucket
            BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
            Map replacementCache = bucket.getReplacementCache();
            if(replacementCache!= null &&
                    replacementCache.size()!=0 && 
                    replacementCache.containsKey(nodeId)) {
                existingNode = (ContactNode) replacementCache.get(nodeId);
                replacement = true;
            }
            else {
                return false;
            }
        }
        //TODO do some contact checking here first!
        if(alive) {
            existingNode.alive();
            //TODO: we have found a live contact in the bucket's replacement cache!
            //It's a good time to replace this bucket's dead entry with this node
            if(replacement) {
                
            }
        }
        //TODO update the contact's info here
        
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Replaced existing node: "+existingNode+" with node: " 
                    + node);
        }
        return true;
    }
    

    public void refreshBuckets() throws IOException {
        long now = System.currentTimeMillis();
        List buckets = bucketsTrie.values();
        for (Iterator iter = buckets.iterator(); iter.hasNext();) {
            BucketNode bucket = (BucketNode) iter.next();
            long delay = now - bucket.getTimeStamp();
            if(delay > refreshLimit) {
                //select a random ID with this prefix
                KUID randomID = KUID.createRandomID(bucket.getNodeID().getBytes(),bucket.getDepth());
                //TODO: properly request the lookup
                context.lookup(randomID,null);
            }
        }
    }

    public void clear() {
        nodesTrie.clear();
        bucketsTrie.clear();
        staleNodes.clear();
    }

    public boolean containsNode(KUID nodeId) {
        return nodesTrie.containsKey(nodeId);
    }

    public ContactNode get(KUID nodeId, boolean checkAndUpdateCache) {
        BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
        ContactNode node = (ContactNode)nodesTrie.get(nodeId);
        if (node == null && checkAndUpdateCache) {
            node = (ContactNode)bucket.getReplacementNode(nodeId);
        }
        return node;
    }

    public ContactNode get(KUID nodeId) {
        return get(nodeId, false);
    }

    public Collection getAllNodes() {
        return nodesTrie.values();
    }

    
    
    public Collection getAllBuckets() {
        return bucketsTrie.values();
    }

    /**
     * Increments ContactNode's failure counter, marks it as stale
     * if a certain error level is exceeded and returns 
     * true if it's the case.
     */
    public boolean handleFailure(ContactNode node) {
        if (node != null) {
            if (node.failure() >= RouteTableSettings.getMaxNodeFailures()) {
                staleNodes.put(node.getNodeID(), node);
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return nodesTrie.isEmpty();
    }

    public void remove(KUID key) {
        staleNodes.remove(key);
        //TODO mark: some logic to delete a bucket if it's empty
        BucketNode bucket = (BucketNode)bucketsTrie.select(key);
        bucket.decrementNodeCount();
        bucket.removeReplacementNode(key);
        nodesTrie.remove(key);
    }

    public List select(KUID lookup, int k, boolean skipStale) {
        touchBucket(lookup);
        if (skipStale) {
            return nodesTrie.select(lookup, staleNodes.keySet(), k);
        } else {
            return nodesTrie.select(lookup, Collections.EMPTY_SET, k);
        }
    }
    
    /** 
     * Returns a List of buckts sorted by their 
     * closeness to the provided Key. Use BucketList's
     * sort method to sort the Nodes by last-recently 
     * and most-recently seen.
     */
    public List select(KUID lookup, int k) {
        return select(lookup, k, false);
    }

    public synchronized List select(KUID lookup, KUID excludeKey, int k, boolean skipStale) {
        touchBucket(lookup);
        Collection exclude = Collections.EMPTY_SET;
        if (skipStale) {
            exclude = staleNodes.keySet();
            if (excludeKey != null && !staleNodes.containsKey(excludeKey)) {
                exclude = new StaleExcludeDelegate(staleNodes.keySet(), excludeKey);
            }
        } else {
            if (excludeKey != null) {
                exclude = new HashSet();
                exclude.add(excludeKey);
            }
        }
        return nodesTrie.select(lookup, exclude, k);
    }

    public List select(KUID lookup, KUID excludeKey, int k) {
        return select(lookup, excludeKey, k, false);
    }

    public ContactNode select(KUID key) {
        touchBucket(key);
        return (ContactNode)nodesTrie.select(key);
    }
    
    public int size() {
        return nodesTrie.size();
    }

    public boolean updateTimeStamp(ContactNode node) {
        
        //TODO change this!!!!
        if (node != null) {
            node.alive();
            staleNodes.remove(node.getNodeID());
            updateIfCached(node.getNodeID());
            return true;
        }
        return false;
    }
    
    private boolean updateIfCached(KUID key) {
        BucketNode bucket = (BucketNode)bucketsTrie.select(key);
        return bucket.getReplacementNode(key) != null;
    }
    
    private void touchBucket(KUID nodeId) {
        //      get bucket closest to node
        BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
        bucket.touch();
    }
    
    

    
    /**
     * A combined Stale ContactNode and excude ContactNode Delegate.
     * Pass it to the Trie to exclude all stale Nodes
     * and one specific ContactNode.
     */
    private static class StaleExcludeDelegate implements Collection {
        
        private Set stale;
        private KUID exclude;
        
        public StaleExcludeDelegate(Set stale, KUID exclude) {
            this.stale = stale;
            this.exclude = exclude;
        }

        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object o) {
            return stale.contains(o) || exclude.equals(o);
        }

        public boolean containsAll(Collection c) {
            for(Iterator it = c.iterator(); it.hasNext(); ) {
                if (!contains(it.next())) {
                    return false;
                }
            }
            return true;
        }

        public boolean isEmpty() {
            return false;
        }

        public Iterator iterator() {
            return Collections.EMPTY_LIST.iterator();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            int size = stale.size();
            if (!stale.contains(exclude)) {
                size++;
            }
            return size;
        }

        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray(Object[] a) {
            throw new UnsupportedOperationException();
        }
    }
}
