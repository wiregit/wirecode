package de.kapsi.net.kademlia.routing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.security.krb5.internal.crypto.b;

import de.kapsi.net.kademlia.BucketNode;
import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.RouteTableSettings;
import de.kapsi.net.kademlia.util.BucketUtils;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class PatriciaRouteTable implements RoutingTable {
    
    private static final Log LOG = LogFactory.getLog(PatriciaRouteTable.class);
    
    private static final int K = KademliaSettings.getReplicationParameter();
    
    private static final int B = RouteTableSettings.getDepthLimit();
    
    private static final long refreshLimit = RouteTableSettings.getBucketRefreshTime();
    
    private Context context;
    
    private PatriciaTrie nodesTrie;
    
    private PatriciaTrie bucketsTrie;

    private BucketNode smallestSubtreeBucket;
    
    public PatriciaRouteTable(Context context) {
        this.context = context;
        
        nodesTrie = new PatriciaTrie();
        bucketsTrie = new PatriciaTrie();
        
        init();
    }
    
    private void init() {
        KUID rootKUID = KUID.MIN_NODE_ID;
        BucketNode root = new BucketNode(rootKUID,0);
        bucketsTrie.put(rootKUID,root);
    }
    
    public boolean load() {
        File file = new File(RouteTableSettings.ROUTETABLE_FILE);
        if (file.exists() && file.isFile() && file.canRead()) {
            
            ObjectInputStream in = null;
            try {
                FileInputStream fin = new FileInputStream(file);
                GZIPInputStream gzin = new GZIPInputStream(fin);
                in = new ObjectInputStream(gzin);
                
                KUID nodeId = (KUID)in.readObject();
                if (!nodeId.equals(context.getLocalNodeID())) {
                    return false;
                }
                
                PatriciaTrie bucketsTrie = (PatriciaTrie)in.readObject();
                PatriciaTrie nodeTrie = (PatriciaTrie)in.readObject();
                this.bucketsTrie = bucketsTrie;
                this.nodesTrie = nodeTrie;
                
                //refresh the buckets
                refreshBuckets(true);
                
                return true;
            } catch (FileNotFoundException e) {
                LOG.error(e);
            } catch (IOException e) {
                LOG.error(e);
            } catch (ClassNotFoundException e) {
                LOG.error(e);
            } finally {
                try { if (in != null) { in.close(); } } catch (IOException ignore) {}
            }
        }
        return false;
    }
    
    public boolean store() {
        File file = new File(RouteTableSettings.ROUTETABLE_FILE);
        
        ObjectOutputStream out = null;
        
        try {
            FileOutputStream fos = new FileOutputStream(file);
            GZIPOutputStream gzout = new GZIPOutputStream(fos);
            out = new ObjectOutputStream(gzout);
            out.writeObject(context.getLocalNodeID());
            out.writeObject(bucketsTrie);
            out.writeObject(nodesTrie);
            out.flush();
            return true;
        } catch (FileNotFoundException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        } finally {
            try { if (out != null) { out.close(); } } catch (IOException ignore) {}
        }
        return false;
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
            
            if(knowToBeAlive) {
                node.alive();
            }
            
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
            boolean partOfSmallest = (smallestSubtreeBucket!= null) && smallestSubtreeBucket.equals(bucket);
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
                //update smallest subtree if split reason was that it contained the local bucket
                if(containsLocal) {
                    BucketNode newLocalBucket = (BucketNode)bucketsTrie.select(context.getLocalNodeID());
                    if(newLocalBucket.equals(leftSplitBucket)) {
                        smallestSubtreeBucket = rightSplitBucket;
                    }
                    else if(newLocalBucket.equals(rightSplitBucket)){
                        smallestSubtreeBucket = leftSplitBucket;
                    } else {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("New buckets don't contain local node");
                        }
                        return;
                    }
                }
                //now trying recursive call!
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
        
        if(nodeId == null) return;
        
        //this should never happen -- who knows?!!
        if(nodeId.equals(context.getLocalNodeID())) {
            if(LOG.isErrorEnabled()) {
                LOG.error("Local node marked as dead!");
            }
        }
        ContactNode node = (ContactNode) nodesTrie.get(nodeId);
        //get closest bucket
        BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
        if(node != null) {
            //only remove if node considered stale and the bucket is full and it's replacement cache is not empty
            if((node.failure() > RouteTableSettings.getMaxNodeFailures())
                    && (bucket.getNodeCount() >= K) 
                    && (bucket.getReplacementCacheSize() > 0)) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removing node: "+node+" from bucket: "+bucket);
                }
                //remove node and replace with most recent alive one from cache
                nodesTrie.remove(nodeId);
                bucket.decrementNodeCount();
                ContactNode replacement = bucket.getMostRecentlySeenCachedNode(true);
                put(replacement.getNodeID(),replacement,false);
            }
        } else {
                node = bucket.removeReplacementNode(nodeId);
                if (node!= null && LOG.isTraceEnabled()) {
                    LOG.trace("Removed node: "+node+" from replacement cache");
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
     * and ping the last recently node
     * 
     * @param bucket
     * @param node
     */
    public void addReplacementNode(BucketNode bucket,ContactNode node) {
        
        boolean added = false;
        
        Map replacementCache = bucket.getReplacementCache();

        //first add to the replacement cache
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
        //a good time to ping least recently seen node
        if(added) {
            pingBucketLastRecentlySeenNode(bucket);
        }
    }
    
    private void pingBucketLastRecentlySeenNode(BucketNode bucket) {
        
        if(bucket == null) return;
        
        int depth = bucket.getDepth();
        List bucketList;
        if(depth < 1) {
            //we are selecting from the root
            bucketList = nodesTrie.range(bucket.getNodeID(),0);
        } else {
            bucketList = nodesTrie.range(bucket.getNodeID(),bucket.getDepth()-1);
        }
        ContactNode leastRecentlySeen = 
            BucketUtils.getLeastRecentlySeen(BucketUtils.sort(bucketList));
        
        //don't ping ourselves
        if(leastRecentlySeen.equals(context.getLocalNode())) {
            return;
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Pinging the least recently seen Node " 
                    + leastRecentlySeen);
        }
        
        PingRequest ping = context.getMessageFactory().createPingRequest();
        try {
            //will get handled by DefaultMessageHandler
            context.getMessageDispatcher().send(leastRecentlySeen, ping, null);
        } catch (IOException e) {}
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
        BucketNode bucket = null;
        ContactNode existingNode = (ContactNode) nodesTrie.get(nodeId);
        if(existingNode == null) {
            //check replacement cache in closest bucket
            bucket = (BucketNode)bucketsTrie.select(nodeId);
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
                pingBucketLastRecentlySeenNode(bucket);
            }
        }
        //TODO update the contact's info here
        
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Replaced existing node: "+existingNode+" with node: " 
                    + node);
        }
        return true;
    }
    
    public void refreshBuckets(boolean force) throws IOException{
        refreshBuckets(force,null);
    }
    
    public void refreshBuckets(boolean force,BootstrapListener l) throws IOException{
        ArrayList bucketsLookups = new ArrayList();
        long now = System.currentTimeMillis();
        List buckets = bucketsTrie.values();
        for (Iterator iter = buckets.iterator(); iter.hasNext();) {
            BucketNode bucket = (BucketNode) iter.next();
            long lastTouch = bucket.getTimeStamp();
            //update bucket if freshness limit has passed
            //OR if it is not full (not complete)
            //OR if there is at least one invalid node inside
            //OR if forced
            int depth = bucket.getDepth();
            List liveNodes;
            if(depth < 1) {
                liveNodes = nodesTrie.range(bucket.getNodeID(),0,new NodeAliveKeySelector());
            } else {
                liveNodes = nodesTrie.range(bucket.getNodeID(),bucket.getDepth()-1,new NodeAliveKeySelector());
            }
            if(force || 
                    (now - lastTouch > refreshLimit) || 
                    (bucket.getNodeCount() < K) || 
                    (liveNodes.size() != bucket.getNodeCount())) {
                //select a random ID with this prefix
                KUID randomID = KUID.createPrefxNodeID(bucket.getNodeID().getBytes(),bucket.getDepth());
                
                if(LOG.isTraceEnabled()) {
                    LOG.trace("Refreshing bucket:" + bucket + " with random ID: "+ randomID);
                }
                
                bucketsLookups.add(randomID);
            }
        }
        BootstrapFindNodeListener listener = new BootstrapFindNodeListener(bucketsLookups,l);
        for (Iterator iter = bucketsLookups.iterator(); iter.hasNext();) {
            KUID lookupId = (KUID) iter.next();
            context.lookup(lookupId,listener);
        }
    }
    

    public void clear() {
        nodesTrie.clear();
        bucketsTrie.clear();
        init();
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

    /** 
     * Returns a List of buckets sorted by their 
     * closeness to the provided Key. Use BucketList's
     * sort method to sort the Nodes by last-recently 
     * and most-recently seen.
     */
    public List select(KUID lookup, int k, boolean onlyLiveNodes, boolean isLocalLookup) {
        //only touch bucket if we know we are going to contact it's nodes
        if(isLocalLookup) touchBucket(lookup);
        if(onlyLiveNodes) {
            return nodesTrie.select(lookup, k, new NodeAliveKeySelector());
        }else return nodesTrie.select(lookup, k);
    }


    public ContactNode selectNextClosest(KUID key) {
        return (ContactNode)nodesTrie.selectNextClosest(key);
    }
    
    public ContactNode select(KUID key) {
        return (ContactNode)nodesTrie.select(key);
    }
    
    public int size() {
        return nodesTrie.size();
    }

    private void touchBucket(KUID nodeId) {
        //      get bucket closest to node
        BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Touching bucket: " + bucket);
        }
        
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
    
    private class BootstrapFindNodeListener implements FindNodeListener{

        private List queryList;
        
        private BootstrapListener bootstrapListener;
        
        private boolean foundNodes;
        
        private BootstrapFindNodeListener(List queryList,BootstrapListener listener) {
            this.queryList = queryList;
            this.bootstrapListener = listener;
        }
        
        public synchronized void foundNodes(KUID lookup, Collection nodes, long time) {
            if(!foundNodes && !nodes.isEmpty()) {
                foundNodes = true;
            }
            queryList.remove(lookup);
            
            if(queryList.size() == 0) {
                if(bootstrapListener != null) {
                    bootstrapListener.secondPhaseComplete(time,foundNodes);
                }
            }
        }
        
    }
    
    private class NodeAliveKeySelector implements PatriciaTrie.KeySelector{
        public boolean allow(Object key, Object value) {
            if(value instanceof ContactNode) {
                ContactNode node = (ContactNode)value;
                if(node.hasFailed()) {
                    return false;
                } else return true;
            } else return false;
        }
    }
    
}
