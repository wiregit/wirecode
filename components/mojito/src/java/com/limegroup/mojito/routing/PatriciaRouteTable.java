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
 
package com.limegroup.mojito.routing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.BucketNode;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.Context.BootstrapManager;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.statistics.RoutingStatisticContainer;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.FixedSizeHashMap;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.Trie.KeySelector;


/**
 * This class is a Kademlia DHT routing table implementation 
 * backed by Patricia tries, allowing us to save memory space 
 * and perform fast lookups. The depth of the tree and number 
 * of contacts stored by each node are defined by the replication 
 * parameter K and the the symbol size parameter B.
 *
 */
public class PatriciaRouteTable implements RouteTable {
    
    private static final Log LOG = LogFactory.getLog(PatriciaRouteTable.class);
    
    /**
     * The Kademlia replication parameter.
     */
    private static final int K = KademliaSettings.REPLICATION_PARAMETER.getValue();
    
    /**
     * The symbol size, i.e. the number of bits improved at each step.
     */
    private static final int B = RouteTableSettings.DEPTH_LIMIT.getValue();
    
    /**
     * The <tt>KeySelector</tt> that selects <tt>ContactNodes</tt> with one or more failures.
     */
    private static final KeySelector SELECT_FAILED_CONTACTS = new KeySelector() {
        public boolean allow(Object key, Object value) {
            return ((ContactNode)value).hasFailed();
        }
    };
    
    /**
     * The <tt>KeySelector</tt> that selects <tt>ContactNodes</tt> with no failures.
     */
    private static final KeySelector SELECT_ALIVE_CONTACTS = new KeySelector() {
        public boolean allow(Object key, Object value) {
            return !((ContactNode)value).hasFailed();
        }
    };
    
    /**
     * A <tt>Map</tt> of hosts we contact during the spoof check. 
     * Avoids loops in this process.
     */
    // TODO: Maybe no longer required!
    private Map loopLock = new FixedSizeHashMap(16);
    
    /**
     * The DHT's context.
     */
    private Context context;
    
    /**
     * The main contacts trie.
     */
    private PatriciaTrie nodesTrie;
    
    /**
     * The Buckets trie.
     */
    private PatriciaTrie bucketsTrie;

    /**
     * The bucket of the current smallest subtree on the other side of the local node's bucket.
     */
    private BucketNode smallestSubtreeBucket;
    
    /**
     * The number of consecutive node failures recorded.
     */
    private int consecutiveFailures = 0;
    
    /**
     * The <tt>StatisticsContainer</tt> for the routing table stats.
     */
    private final RoutingStatisticContainer routingStats;
    
    /**
     * Creates a new routing table backed by PatriciaTries
     * 
     * @param context
     */
    public PatriciaRouteTable(Context context) {
        this.context = context;
        
        routingStats = new RoutingStatisticContainer(context);
        
        nodesTrie = new PatriciaTrie();
        bucketsTrie = new PatriciaTrie();
        
        init();
    }
    
    /**
     * Initializes the bucket Trie with an empty Bucket
     */
    private void init() {
        KUID rootKUID = KUID.MIN_NODE_ID;
        BucketNode root = new BucketNode(rootKUID,0);
        bucketsTrie.put(rootKUID,root);
        routingStats.BUCKET_COUNT.incrementStat();
    }
    
    public synchronized boolean add(ContactNode node, boolean knowToBeAlive) {
        return put(node.getNodeID(), node, knowToBeAlive);
    }
    
    /**
     * Puts the contact in the Patricia trie if the bucket corresponding bucket is not full.
     * If the bucket is full, it may be split and open-up space for new contacts.
     * 
     * @param nodeId The KUID of the node to be added
     * @param node The ContactNode 
     * @param knowToBeAlive Did we hear from the node directly or did we learn about it in a FIND_NODE lookup
     * @return true if the node was added, false otherwise
     */
    private boolean put(KUID nodeId, ContactNode node, boolean knowToBeAlive) {

        boolean added = false;
        
        if (nodeId == null) {
            throw new IllegalArgumentException("NodeID is null");
        }
        
        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }
        
        if (!nodeId.equals(node.getNodeID())) {
            throw new IllegalArgumentException("NodeID and the ID returned by Node do not match");
        }
        
        // Don't add firewalled Nodes
        if (node.isFirewalled()) {
            return false;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Trying to add "+(knowToBeAlive?"live":"unknown")+" node: "+node+" to routing table");
        }
        //reset the consecutive failure counter
        consecutiveFailures = 0;
        
        // Update an existing node
        ContactNode existingNode = updateExistingNode(nodeId, node, knowToBeAlive);
        if (existingNode == null) {
            // get bucket closest to node
            BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
            if(bucket.getNodeCount() < K) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Adding node: " + node + " to bucket: " + bucket);
                }
                bucket.incrementNodeCount();
                bucket.removeReplacementNode(nodeId);
                if(knowToBeAlive) {
                    node.alive();
                    routingStats.LIVE_NODE_COUNT.incrementStat();
                    touchBucket(nodeId);
                } else {
                    routingStats.UNKNOWN_NODE_COUNT.incrementStat();
                }
                nodesTrie.put(nodeId, node);
                added = true;
            } else {
            //Three conditions for splitting:
            //1. Bucket contains nodeID.
            //2. New node part of the smallest subtree to the local node
            //2. current_depth mod symbol_size != 0
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
                
                if(containsLocal || partOfSmallest || !tooDeep) {
                    
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("splitting bucket: " + bucket);
                    }
                    
                    List newBuckets = bucket.split();
                    routingStats.BUCKET_COUNT.incrementStat();
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
                        return false;
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
                            return false;
                        }
                    }
                    //now trying recursive call!
                    //attempt the put the new contact again with the split buckets
                    put(nodeId,node,knowToBeAlive);
                    
                } 
                //not splitting --> replace bucket unknown entry or add to replacement cache. Also a good time to replace stale nodes
                else {
                    if(knowToBeAlive) {
                        List bucketList = nodesTrie.range(bucket.getNodeID(), bucket.getDepth()-1);
                        ContactNode leastRecentlySeen = 
                            BucketUtils.getLeastRecentlySeen(BucketUtils.sort(bucketList));
                        if(leastRecentlySeen.getTimeStamp() == 0L) {
                            
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("NOT splitting bucket "+ bucket+", replacing unknown node "+leastRecentlySeen+" with node "+node);
                            }
                            
                            node.alive();
                            routingStats.LIVE_NODE_COUNT.incrementStat();
                            touchBucket(bucket);
                            nodesTrie.remove(leastRecentlySeen.getNodeID());
                            nodesTrie.put(nodeId,node);
                            return true;
                        }
                    } 
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("NOT splitting bucket "+ bucket+", adding node "+node+" to replacement cache");
                    }
                    addReplacementNode(bucket,node);
                    replaceBucketStaleNodes(bucket);
                }
            }
        }         

        return added;
    }
    
    /**
     * Increment the failure count of the corresponding node. 
     * If we have reached the maximum number of failures, evict the node
     * and replace with a node from the replacement cache.
     * 
     */
    public synchronized void handleFailure(KUID nodeId) {
        
        // NodeID might be null if we sent a ping to
        // an unknown Node (i.e. we knew only the
        // address) and the ping failed.
        if (nodeId == null) {
            return;
        }

        //ignore failure if we start getting to many disconnections in a row
        if (consecutiveFailures++ >= RouteTableSettings.MAX_CONSECUTIVE_FAILURES.getValue()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Ignoring node failure as it appears that we are disconnected");
            }
            return;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Handling failure for nodeId: "+nodeId);
        }
        
        //this should never happen -- who knows?!!
        if(context.isLocalNodeID(nodeId)) {
            if(LOG.isErrorEnabled()) {
                LOG.error("Local node marked as dead!");
            }
        }
        
        ContactNode node = (ContactNode) nodesTrie.get(nodeId);
        //get closest bucket
        BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
        if (node != null) {
            //TODO: we delete dead contacts immediately for now!...maybe relax?
            if (handleNodeFailure(node)) {
                removeNodeAndReplace(nodeId, bucket, true);
            }
            /*
            //only remove if node considered stale and the bucket is full and it's replacement cache is not empty
            if(handleNodeFailure(node)
                    && (bucket.getNodeCount() >= K) 
                    && (bucket.getReplacementCacheSize() > 0)) {
                
                //remove node and replace with most recent alive one from cache 
            */
        } else {
            node = bucket.removeReplacementNode(nodeId);
            if(node != null) {
                if (!handleNodeFailure(node)) {
                    bucket.addReplacementNode(node);
                }
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Removed node: "+node+" from replacement cache");
                }
            }
        }
    }
    
    private void replaceBucketStaleNodes(BucketNode bucket) {
        int length = Math.max(0, bucket.getDepth()-1);
        List failingNodes = nodesTrie.range(bucket.getNodeID(),length, SELECT_FAILED_CONTACTS);
        
        for (Iterator iter = failingNodes.iterator(); iter.hasNext(); ) {
            ContactNode failingNode = (ContactNode) iter.next();
            if (!removeNodeAndReplace(failingNode.getNodeID(), bucket, false)) {
                return;
            }
        }
    }
    
    private boolean removeNodeAndReplace(KUID nodeId, BucketNode bucket,boolean force) {
        ContactNode replacement = bucket.getMostRecentlySeenCachedNode(true);
        if (replacement != null) {
            nodesTrie.remove(nodeId);
            bucket.decrementNodeCount();
            put(replacement.getNodeID(),replacement,false);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Replaced nodeId: "+nodeId+" with node "+ replacement +" from bucket: "+bucket);
            }
            touchBucket(bucket);
            return true;
        } else if (force) {
            nodesTrie.remove(nodeId);
            bucket.decrementNodeCount();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Removed nodeId: "+nodeId+" from bucket: "+bucket);
            }
            
            return true;
        } else {
            return false;
        }
    }
    
    private int updateBucketNodeCount(BucketNode bucket) {
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
    private void addReplacementNode(BucketNode bucket, ContactNode node) {
        boolean add = false;
        
        //first add to the replacement cache
        if(bucket.getReplacementCacheSize() 
                >= RouteTableSettings.MAX_CACHE_SIZE.getValue()) {
            
            //replace older cache entries with this one
            Map replacementCache = bucket.getReplacementCache();
            for (Iterator iter = replacementCache.values().iterator(); iter.hasNext();) {
                ContactNode oldNode = (ContactNode) iter.next();
                
                if (oldNode.getTimeStamp() <= node.getTimeStamp()) {
                    iter.remove();
                    add = true;
                    break;
                }
            }
        } else {
            add = true;
        }
        
        //a good time to ping least recently seen node
        if (add) {
            bucket.addReplacementNode(node);
            routingStats.REPLACEMENT_COUNT.incrementStat();
            pingBucketLastRecentlySeenNode(bucket);
        } 
    }
    
    private void pingBucketLastRecentlySeenNode(BucketNode bucket) {
        
        List bucketList = nodesTrie.range(bucket.getNodeID(), bucket.getDepth()-1);
        
        ContactNode leastRecentlySeen = 
            BucketUtils.getLeastRecentlySeen(BucketUtils.sort(bucketList));
        
        //don't ping ourselves or an allready pinged node
        if(leastRecentlySeen.equals(context.getLocalNode())) {
            return;
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Pinging the least recently seen Node " 
                    + leastRecentlySeen);
        }
        
        try {
            //will get handled by DefaultMessageHandler
            context.ping(leastRecentlySeen);
        } catch (IOException e) {
            LOG.error("Pinging the least recently seen Node failed", e);
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
    private ContactNode updateExistingNode(KUID nodeId, ContactNode node, boolean alive) {
        boolean replacement = false;
        BucketNode bucket = null;
        
        // Check the RouteTable for existence
        ContactNode existingNode = (ContactNode) nodesTrie.get(nodeId);
        if (existingNode == null) {
            // check replacement cache in closest bucket
            bucket = (BucketNode)bucketsTrie.select(nodeId);
            existingNode = bucket.getReplacementNode(nodeId);
            
            // If it was neither in the RouteTable nor in the
            // replacement cache then it's new and unknown! We 
            // have to add it first!
            if (existingNode == null) {
                return null;
            }
            
            // ContactNode is from replacement cache!
            replacement = true;
        }

        // if we are here -> the node is already in the routing table
        if (alive) {
            //if the existing node is marked as dead, replace anyway
            if (existingNode.isDead()) {
                return updateContactInfo(existingNode, node, true);
            }
            
            // Same Address? OK, update timestamp
            InetSocketAddress newAddress = (InetSocketAddress) node.getSocketAddress();
            InetSocketAddress oldAddress = (InetSocketAddress) existingNode.getSocketAddress();
            if (oldAddress.equals(newAddress)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Updating timestamp for node: "+existingNode);
                }
                return updateContactInfo(existingNode, node, true);
            }
            
            //check if we have heard of the existing node recently
            long now = System.currentTimeMillis();
            long delay = now - existingNode.getTimeStamp();
            if(delay < RouteTableSettings.MIN_RECONNECTION_TIME.getValue()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Not doing spoof check as contact alive recently for node: "+existingNode);
                }
                return existingNode;
            }
            
            //START: SPOOF CHECK
            try {
                // Huh? The addresses are not equal but both belong
                // obviously to this local machine!? There isn't much
                // we can do. Set it to the new address and hope it 
                // doesn't use a different NIF everytime...
                if (NetworkUtils.isLocalHostAddress(newAddress)
                        && NetworkUtils.isLocalHostAddress(oldAddress)
                        && newAddress.getPort()==oldAddress.getPort()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Local maching loop detection for node: "+existingNode);
                    }
                    return updateContactInfo(existingNode, node, true);
                }
            } catch (IOException ignore) {}
            
            // If a Host has multiple IPs (see also above case) then
            // we may create an infinite loop if boths ends think
            // the other end is trying to spoof its Node ID! Make sure
            // we're not creating a such loop.
            if (loopLock.containsKey(nodeId)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Spoof check already in progress for " + existingNode);
                }
                return existingNode;
            }
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Spoof check for node: "+existingNode);
            }
    
            // Kick off the spoof check. Ping the current contact and
            // if it responds then interpret it as an attempt to spoof
            // the node ID and if it doesn't then it is obviously dead.
            SpoofChecker checker 
                = new SpoofChecker(existingNode, node, replacement, bucket);
            
            try {
                context.ping(existingNode, checker);
                loopLock.put(existingNode.getNodeID(), checker);
            } catch (IOException err) {
                LOG.error("Coud not start spoof check", err);
            }
        } else if(existingNode.isDead()) { //replace anyway and put in unknown state
            return updateContactInfo(existingNode, node, false);
        }
        return existingNode;
    }
    
    private ContactNode updateContactInfo(ContactNode existingNode, ContactNode newNode, boolean alive) {
        if(alive) {
            existingNode.setSocketAddress(newNode.getSocketAddress());
            if(newNode.getRoundTripTime() > 0L) {
                existingNode.setRoundTripTime(newNode.getRoundTripTime());
            }
            existingNode.setInstanceID(newNode.getInstanceID());
            existingNode.alive();
            touchBucket(existingNode.getNodeID());
        } else {
            existingNode.setSocketAddress(newNode.getSocketAddress());
            existingNode.unknownState();
        }
        return existingNode;
    }
        
    public synchronized void refreshBuckets(boolean force) throws IOException{
        refreshBuckets(force, null);
    }
    
    public synchronized void refreshBuckets(boolean force, BootstrapManager manager) throws IOException{
        List bucketsLookups = new ArrayList();
        long now = System.currentTimeMillis();
        
        for (Iterator iter = bucketsTrie.values().iterator(); iter.hasNext(); ) {
            BucketNode bucket = (BucketNode) iter.next();
            
            long lastTouch = bucket.getTimeStamp();
            //update bucket if freshness limit has passed
            //OR if it is not full (not complete)
            //OR if there is at least one invalid node inside
            //OR if forced 
            //TODO: maybe relax this a little bit?
            int length = Math.max(0, bucket.getDepth()-1);
            List liveNodes = nodesTrie.range(bucket.getNodeID(), length, SELECT_ALIVE_CONTACTS);
            
            //if we are bootstrapping, phase 1 allready took care of the local bucket
            if(manager != null && liveNodes.contains(context.getLocalNode())) {
                continue;
            }
            
            if(force || ((now - lastTouch) > RouteTableSettings.BUCKET_REFRESH_TIME.getValue()) 
                    || (bucket.getNodeCount() < K) 
                    || (liveNodes.size() != bucket.getNodeCount())) {
                //select a random ID with this prefix
                KUID randomID = KUID.createPrefxNodeID(bucket.getNodeID().getBytes(),bucket.getDepth());
                
                if(LOG.isTraceEnabled()) {
                    LOG.trace("Refreshing bucket:" + bucket + " with random ID: "+ randomID);
                }
                
                bucketsLookups.add(randomID);
            }
        }
        
        if (manager != null) {
            manager.setBuckets(bucketsLookups);
        }
        
        for (Iterator iter = bucketsLookups.iterator(); iter.hasNext(); ) {
            context.lookup((KUID) iter.next(), manager);
            routingStats.BUCKET_REFRESH_COUNT.incrementStat();
        }
    }
    
    public synchronized void clear() {
        nodesTrie.clear();
        bucketsTrie.clear();
        loopLock.clear();
        
        init(); // init the Bucket Trie!
    }

    public synchronized boolean containsNode(KUID nodeId) {
        return nodesTrie.containsKey(nodeId);
    }

    public synchronized ContactNode get(KUID nodeId, boolean checkAndUpdateCache) {
        ContactNode node = (ContactNode)nodesTrie.get(nodeId);
        if (node == null && checkAndUpdateCache) {
            BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
            node = (ContactNode)bucket.getReplacementNode(nodeId);
        }
        return node;
    }

    public synchronized ContactNode get(KUID nodeId) {
        return get(nodeId, false);
    }

    public synchronized List getAllNodes() {
        return nodesTrie.values();
    }
    
    public synchronized List getAllNodesMRS() {
        List nodesList = nodesTrie.values();
        return BucketUtils.sort(nodesList);
    }
    
    public synchronized List getMRSNodes(int numNodes) {
        List nodesList = getAllNodesMRS();
        return nodesList.subList(0,Math.min(nodesList.size(),numNodes));
    }

    public synchronized List getAllBuckets() {
        return bucketsTrie.values();
    }

    /**
     * Increments ContactNode's failure counter, marks it as stale
     * if a certain error level is exceeded and returns 
     * true if it's the case.
     */
    private boolean handleNodeFailure(ContactNode node) {
        if ((node != null) && node.failure()) {
            routingStats.DEAD_NODE_COUNT.incrementStat();
            return true;
        }
        return false;
    }

    public synchronized boolean isEmpty() {
        return nodesTrie.isEmpty();
    }

    /** 
     * Returns a List of ContactNodes sorted by their 
     * closeness to the provided Key. Use BucketList's
     * sort method to sort the Nodes from least-recently 
     * to most-recently seen.
     */
    public synchronized List select(KUID lookup, int k, boolean onlyLiveNodes, boolean willContact) {
        //only touch bucket if we know we are going to contact it's nodes
        if(willContact) {
            touchBucket(lookup);
        }
        
        if(onlyLiveNodes) {
            return nodesTrie.select(lookup, k, SELECT_ALIVE_CONTACTS);
        } else {
            return nodesTrie.select(lookup, k);
        }
    }
    
    public synchronized ContactNode select(KUID lookup) {
        return (ContactNode)nodesTrie.select(lookup);
    }
    
    public synchronized int size() {
        return nodesTrie.size();
    }

    public synchronized int getBucketCount() {
        return bucketsTrie.size();
    }
    
    private void touchBucket(KUID nodeId) {
        //get bucket closest to node
        BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
        touchBucket(bucket);
    }
    
    private void touchBucket(BucketNode bucket) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Touching bucket: " + bucket);
        }
        
        bucket.touch();
    }
    
    public synchronized String toString() {
        Collection bucketsList = getAllBuckets();
        StringBuffer buffer = new StringBuffer("\n");
        buffer.append("-------------\nLocal node:"+context.getLocalNode()+"\n");
        buffer.append("-------------\nBuckets:\n");
        int totalNodesInBuckets = 0;
        for(Iterator it = bucketsList.iterator(); it.hasNext(); ) {
            BucketNode bucket = (BucketNode)it.next();
            buffer.append(bucket).append("\n");
            totalNodesInBuckets += bucket.getNodeCount();
            Collection bucketNodes = nodesTrie.range(bucket.getNodeID(),bucket.getDepth()-1);
            buffer.append("\tNodes:"+"\n");
            for (Iterator iter = bucketNodes.iterator(); iter.hasNext();) {
                ContactNode node = (ContactNode) iter.next();
                buffer.append("\t"+node+"\n");
            }
        }
        buffer.append("-------------\n");
        buffer.append("TOTAL BUCKETS: " + bucketsList.size()).append(" NUM. OF NODES: "+totalNodesInBuckets+"\n");
        buffer.append("-------------\n");
        
        Collection nodesList = getAllNodes();
        buffer.append("-------------\n");
        buffer.append("TOTAL NODES: " + nodesList.size()).append("\n");
        buffer.append("-------------\n");
        return buffer.toString();
    }
    
    /**
     * The SpoofChecker makes sure two Nodes cannot have the same
     * NodeID at the same time.
     */
    private class SpoofChecker implements PingListener {
        
        private ContactNode currentContact;
        private ContactNode newContact;
        
        private boolean replacementNode;
        private BucketNode replacementBucket;
        
        private SpoofChecker(ContactNode currentContact, ContactNode newContact, 
                boolean replacementNode, BucketNode replacementBucket) {
            
            this.currentContact = currentContact;
            this.newContact = newContact;
            this.replacementNode = replacementNode;
            this.replacementBucket = replacementBucket;
        }
        
        public void response(ResponseMessage response, long time) {
            ContactNode node = response.getContactNode();
            loopLock.remove(node.getNodeID());
            touchBucket(node.getNodeID());
            
            if (LOG.isWarnEnabled()) {
                LOG.warn("WARNING: " + newContact + " is trying to spoof its NodeID. " 
                        + response.getContactNode()
                        + " responded in " + time + " ms");
            }
            routingStats.SPOOF_COUNT.incrementStat();
            // Do nothing else! DefaultMessageHandler takes
            // care of everything else!
            //TODO: add bad node to IP Filter
        }

        public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
            loopLock.remove(nodeId);
            
            // The current contact is obviously not responding
            if (LOG.isInfoEnabled()) {
                LOG.info(currentContact + " does not respond! Replacing it with " + newContact);
            }
            updateContactInfo(currentContact, newContact, true);
            
            if(replacementNode && replacementBucket != null) {
                // we have found a live contact in the bucket's replacement cache!
                // It's a good time to replace this bucket's dead entry with this node
                pingBucketLastRecentlySeenNode(replacementBucket);
            }
        }
    }
}
