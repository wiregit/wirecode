package de.kapsi.net.kademlia.routing;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    public PatriciaRouteTable(Context context) {
        this.context = context;
        
        nodesTrie = new PatriciaTrie();
        bucketsTrie = new PatriciaTrie();
        
        KUID rootKUID = KUID.MIN_NODE_ID;
        BucketNode root = new BucketNode(rootKUID,0);
        bucketsTrie.put(rootKUID,root);
    }

    public void add(ContactNode node, boolean knowToBeAlive) {
        put(node.getNodeID(), node,knowToBeAlive);
    }
    
    private void put(KUID nodeId, ContactNode node, boolean knowToBeAlive) {
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
        
        if(updateExistingNode(nodeId,node,knowToBeAlive)) {
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
            if(knowToBeAlive)node.alive();
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
                put(nodeId,node,knowToBeAlive);
                
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
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return nodesTrie.isEmpty();
    }

    public void remove(KUID key) {
        //TODO mark: some logic to delete a bucket if it's empty
        BucketNode bucket = (BucketNode)bucketsTrie.select(key);
        bucket.decrementNodeCount();
        bucket.removeReplacementNode(key);
        nodesTrie.remove(key);
    }

    /** 
     * Returns a List of buckts sorted by their 
     * closeness to the provided Key. Use BucketList's
     * sort method to sort the Nodes by last-recently 
     * and most-recently seen.
     */
    public List select(KUID lookup, int k) {
        touchBucket(lookup);
        return nodesTrie.select(lookup, k);
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

    public String toString() {
        Collection bucketsList = getAllBuckets();
        StringBuffer buffer = new StringBuffer("\n");
        buffer.append("-------------\nBuckets:\n");
        int totalNodesInBuckets = 0;
        for(Iterator it = bucketsList.iterator(); it.hasNext(); ) {
            BucketNode bucket = (BucketNode)it.next();
            buffer.append(bucket).append("\n");
            totalNodesInBuckets += bucket.getNodeCount();
        }
        buffer.append("-------------\n");
        buffer.append("TOTAL BUCKETS: " + bucketsList.size()).append(" NUM. OF NODES: "+totalNodesInBuckets+"\n");
        buffer.append("-------------\n");
        
        Collection nodesList = getAllNodes();
        buffer.append("-------------\nNodes:\n");
        for(Iterator it = nodesList.iterator(); it.hasNext(); ) {
            ContactNode node = (ContactNode)it.next();
            
            buffer.append(node).append("\n");
        }
        buffer.append("-------------\n");
        buffer.append("TOTAL NODES: " + nodesList.size()).append("\n");
        buffer.append("-------------\n");
        return buffer.toString();
    }
    
    
}
