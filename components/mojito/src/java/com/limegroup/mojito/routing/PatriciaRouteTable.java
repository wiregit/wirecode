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
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.BucketNode;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.BootstrapManager;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.settings.RouteTableSettings;
import com.limegroup.mojito.statistics.RoutingStatisticContainer;
import com.limegroup.mojito.util.BucketUtils;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.TrieUtils;
import com.limegroup.mojito.util.Trie.Cursor;

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
    private static final Cursor<KUID, ContactNode> SELECT_FAILED_CONTACTS = new Cursor<KUID, ContactNode>() {
        public boolean select(Entry<KUID, ContactNode> entry) {
            return entry.getValue().hasFailed();
        }
    };
    
    /**
     * The <tt>KeySelector</tt> that selects <tt>ContactNodes</tt> with no failures.
     */
    private static final Cursor<KUID, ContactNode> SELECT_ALIVE_CONTACTS = new Cursor<KUID, ContactNode>() {
        public boolean select(Entry<KUID, ContactNode> entry) {
            return !entry.getValue().hasFailed();
        }
    };
    
    /**
     * The DHT's context.
     */
    protected Context context;
    
    /**
     * The main contacts trie.
     */
    private PatriciaTrie<KUID, ContactNode> nodesTrie;
    
    /**
     * The Buckets trie.
     */
    private PatriciaTrie<KUID, BucketNode> bucketsTrie;

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
        
        nodesTrie = new PatriciaTrie<KUID, ContactNode>();
        bucketsTrie = new PatriciaTrie<KUID, BucketNode>();
        
        init();
    }
    
    /**
     * Initializes the bucket Trie with an empty Bucket
     */
    private void init() {
        KUID rootKUID = KUID.MIN_NODE_ID;
        BucketNode root = new BucketNode(rootKUID, 0);
        bucketsTrie.put(rootKUID, root);
        routingStats.BUCKET_COUNT.incrementStat();
        
        consecutiveFailures = 0;
        smallestSubtreeBucket = null;
    }
    
    /**
     * Adds the contact to the Patricia trie if the corresponding bucket is not full.
     * If the bucket is full, it may be split and open-up space for new contacts.
     * 
     * @param nodeId The KUID of the node to be added
     * @param node The ContactNode 
     * @param knowToBeAlive Did we hear from the node directly or did we learn about it in a FIND_NODE lookup
     * @return true if the node was added, false otherwise
     */
    public synchronized boolean add(ContactNode node, boolean knowToBeAlive) {

        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }
        
        // Don't add firewalled Nodes
        if (node.isFirewalled()) {
            return false;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Trying to add " + (knowToBeAlive ? "live" : "unknown") 
                    + " node: " + node + " to routing table");
        }
        //reset the consecutive failure counter
        consecutiveFailures = 0;
        
        // Update an existing node
        ContactNode existingNode = updateIfExistingNode(node, knowToBeAlive);
        
        // IF null then it's a new ContactNode!
        if (existingNode == null) {
            // Get the Bucket closest to node
            BucketNode bucket = bucketsTrie.select(node.getNodeID());
            
            // The Bucket is NOT full? Just add it!
            if(!bucket.isFull()) {
                return addToBucket(bucket, node, knowToBeAlive);
            
            // The Bucket IS full! Try to split it and re-add recursively!
            } else if ( splitBucket(bucket) ) {
                return add(node, knowToBeAlive);
                     
            // The Bucket did not split! Replace an unknown entry or 
            // add the new contact to the replacement cache. This is also
            // a good time to replace stale nodes!
            } else {
                return replaceInBucket(bucket, node, knowToBeAlive);
            }
        }

        return false;
    }
    
    /**
     * Checks the local table and replacement nodes and updates timestamp.
     * 
     * @param nodeId The contact nodeId
     * @param node The contact node 
     * @param isAlive If the contact is alive
     * 
     * @return true if the contact exists and has been updated, false otherwise
     */
    private ContactNode updateIfExistingNode(ContactNode node, boolean isAlive) {
        boolean fromReplacementCache = false;
        BucketNode bucket = null;
        
        KUID nodeId = node.getNodeID();
        
        // Check the RouteTable for existence
        ContactNode existingNode = nodesTrie.get(nodeId);
        if (existingNode == null) {
            // check replacement cache in closest bucket
            bucket = bucketsTrie.select(nodeId);
            existingNode = bucket.getReplacementNode(nodeId);
            
            // If it was neither in the RouteTable nor in the
            // replacement cache then it's new and unknown! We 
            // have to add it first!
            if (existingNode == null) {
                return null;
            }
            
            // ContactNode is from replacement cache!
            fromReplacementCache = true;
        }

        // if we are here -> the node is already in the routing table
        if (isAlive) {
            
            // IF the existing node is marked as dead replace it!
            if (existingNode.isDead()) {
                return updateContactInfo(existingNode, node, true);
            }
            
            // ELSE IF same Address? Update timestamp!
            InetSocketAddress newAddress = (InetSocketAddress) node.getSocketAddress();
            InetSocketAddress oldAddress = (InetSocketAddress) existingNode.getSocketAddress();
            if (oldAddress.equals(newAddress)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Updating timestamp for node: "+existingNode);
                }
                return updateContactInfo(existingNode, node, true);
            }
            
            // ELSE IF we have heard of the existing node recently do nothing!
            long now = System.currentTimeMillis();
            long delay = now - existingNode.getTimeStamp();
            if(delay < RouteTableSettings.MIN_RECONNECTION_TIME.getValue()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Not doing spoof check as contact alive recently for node: " + existingNode);
                }
                return existingNode;
            }
            
            // ELSE 
            try {
                // Huh? The addresses are not equal but both belong
                // obviously to this local machine!? There isn't much
                // we can do. Set it to the new address and hope it 
                // doesn't use a different NIF everytime...
                if (NetworkUtils.isLocalHostAddress(newAddress)
                        && NetworkUtils.isLocalHostAddress(oldAddress)
                        && newAddress.getPort() == oldAddress.getPort()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Local machine is using different NIFs" + existingNode);
                    }
                    return updateContactInfo(existingNode, node, true);
                }
            } catch (IOException ignore) {}
            
            // ELSE start spoof check!
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Spoof check for node: " + existingNode);
            }
    
            // Kick off the spoof check. Ping the current contact and
            // if it responds then interpret it as an attempt to spoof
            // the node ID and if it doesn't then it is obviously dead.
            try {
                context.spoofCheckPing(existingNode, 
                        new SpoofCheckerImpl(existingNode, node, fromReplacementCache, bucket));
            } catch (IOException err) {
                LOG.error("Could not start spoof check", err);
            }
        
        // Replace the existing Node if it's dead!
        } else if(existingNode.isDead()) { 
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
    
    /**
     * Adds the ContactNode to the Bucket.
     * 
     * Returns always true.
     */
    private boolean addToBucket(BucketNode bucket, ContactNode node, boolean knowToBeAlive) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding node: " + node + " to bucket: " + bucket);
        }
        
        KUID nodeId = node.getNodeID();
        
        bucket.incrementNodeCount();
        bucket.removeReplacementNode(nodeId);
        
        if (knowToBeAlive) {
            node.alive();
            routingStats.LIVE_NODE_COUNT.incrementStat();
            touchBucket(nodeId);
        } else {
            routingStats.UNKNOWN_NODE_COUNT.incrementStat();
        }
        
        nodesTrie.put(nodeId, node);
        return true;
    }
    
    /**
     * Tries to split the Bucket into two new parts. Returns true 
     * if the Bucket was split and false otherwise.
     */
    private boolean splitBucket(BucketNode bucket) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Bucket " + bucket + " full");
        }
        
        // Three conditions for splitting:
        // 1. Bucket contains nodeID.
        // 2. New node part of the smallest subtree to the local node
        // 3. current_depth mod symbol_size != 0
        
        BucketNode localBucket = bucketsTrie.select(context.getLocalNodeID());
        
        //1
        boolean containsLocal = localBucket.equals(bucket);
        
        //2
        boolean partOfSmallest = (smallestSubtreeBucket!= null) && smallestSubtreeBucket.equals(bucket);
        
        //3
        boolean tooDeep = bucket.getDepth() % B == 0;
        
        if (containsLocal || partOfSmallest || !tooDeep) {
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("splitting bucket: " + bucket);
            }
            
            List<BucketNode> newBuckets = bucket.split();
            routingStats.BUCKET_COUNT.incrementStat();
            
            //update bucket node count
            BucketNode leftSplitBucket = newBuckets.get(0);
            BucketNode rightSplitBucket = newBuckets.get(1);
            bucketsTrie.put(leftSplitBucket.getNodeID(), leftSplitBucket);
            bucketsTrie.put(rightSplitBucket.getNodeID(), rightSplitBucket);
            int countLeft = updateNodeCountOfBucket(leftSplitBucket);
            int countRight = updateNodeCountOfBucket(rightSplitBucket);
            
            //this should never happen
            if(countLeft + countRight != bucket.getNodeCount()) {
                if (LOG.isFatalEnabled()) {
                    LOG.fatal("Bucket did not split correctly!");
                }
                return false;
            }
            
            //update smallest subtree if split reason was that it contained the local bucket
            if (containsLocal) {
                BucketNode newLocalBucket = bucketsTrie.select(context.getLocalNodeID());
                if(newLocalBucket.equals(leftSplitBucket)) {
                    smallestSubtreeBucket = rightSplitBucket;
                    
                } else if(newLocalBucket.equals(rightSplitBucket)){
                    smallestSubtreeBucket = leftSplitBucket;
                    
                } else {
                    if (LOG.isFatalEnabled()) {
                        LOG.fatal("New buckets don't contain local node");
                    }
                    return false;
                }
            }
            
            // WHOHOOO! WE SPLIT THE BUCKET!!!
            return true;
        }
        
        return false;
    }
    
    private int updateNodeCountOfBucket(BucketNode bucket) {
        int newCount = nodesTrie.range(bucket.getNodeID(),bucket.getDepth()-1).size();
        bucket.setNodeCount(newCount);
        return newCount;
    }
    
    /**
     * Tries to replace an existing but unknown contact in the bucket 
     * with a live contact.
     * 
     * Returns true if a contact was replaced by the new contact.
     */
    private boolean replaceInBucket(BucketNode bucket, ContactNode node, boolean knowToBeAlive) {
        if (knowToBeAlive) {
            List<ContactNode> bucketList = nodesTrie.range(bucket.getNodeID(), bucket.getDepth()-1);
            ContactNode leastRecentlySeen = 
                BucketUtils.getLeastRecentlySeen(BucketUtils.sort(bucketList));
            
            if (leastRecentlySeen.getTimeStamp() == 0L) {
                
                if (LOG.isTraceEnabled()) {
                    LOG.trace("NOT splitting bucket "+ bucket+", replacing unknown node " 
                            + leastRecentlySeen+" with node "+node);
                }
                
                node.alive();
                routingStats.LIVE_NODE_COUNT.incrementStat();
                touchBucket(bucket);
                nodesTrie.remove(leastRecentlySeen.getNodeID());
                nodesTrie.put(node.getNodeID(), node);
                return true;
            }
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("NOT splitting bucket "+ bucket+", adding node "+node+" to replacement cache");
        }
        
        addReplacementNode(bucket, node);
        replaceStaleNodesInBucket(bucket);
        return false;
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
            Collection<ContactNode> nodes = bucket.getReplacementCache().values();
            for (Iterator<ContactNode> iter = nodes.iterator(); iter.hasNext(); ) {
                ContactNode oldNode = iter.next();
                
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
            pingLeastRecentlySeenNodeInBucket(bucket);
        } 
    }
    
    private void pingLeastRecentlySeenNodeInBucket(BucketNode bucket) {
        
        List<ContactNode> bucketList = nodesTrie.range(bucket.getNodeID(), bucket.getDepth()-1);
        
        ContactNode leastRecentlySeen = 
            BucketUtils.getLeastRecentlySeen(BucketUtils.sort(bucketList));
        
        //don't ping ourselves or an allready pinged node
        if (leastRecentlySeen.equals(context.getLocalNode())) {
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

    private void replaceStaleNodesInBucket(BucketNode bucket) {
        int length = Math.max(0, bucket.getDepth()-1);
        List<ContactNode> failingNodes = nodesTrie.range(bucket.getNodeID(), length, SELECT_FAILED_CONTACTS);
        
        for (ContactNode node : failingNodes) {
            if (!removeNodeAndReplace(node.getNodeID(), bucket, false)) {
                return;
            }
        }
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
        
        ContactNode node = nodesTrie.get(nodeId);
        //get closest bucket
        BucketNode bucket = bucketsTrie.select(nodeId);
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
    
    /**
     * Increments ContactNode's failure counter, marks it as stale
     * if a certain error level is exceeded and returns 
     * true if it's the case.
     */
    private boolean handleNodeFailure(ContactNode node) {
        if (node != null && node.failure()) {
            routingStats.DEAD_NODE_COUNT.incrementStat();
            return true;
        }
        return false;
    }
    
    /**
     * Removes the given node from the nodes trie and replaces it with the most recently seen
     * node from the given bucket's replacement cache. If <b>force</b> is set to false, this method
     * only removes the node if there is a replacement node available.
     * 
     * @param nodeId the node to remove
     * @param bucket the bucket where the given node belongs
     * @param force true to remove node in any case, false to remove only if a replacement is available
     * @return true if the node was removed, false otherwise
     */
    private boolean removeNodeAndReplace(KUID nodeId, BucketNode bucket, boolean force) {
        ContactNode replacement = bucket.getMostRecentlySeenCachedNode(true);
        if (replacement != null) {
            nodesTrie.remove(nodeId);
            bucket.decrementNodeCount();
            add(replacement, false);
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Replaced nodeId: " + nodeId + " with node " 
                        + replacement + " from bucket: "+bucket);
            }
            touchBucket(bucket);
            return true;
        } else if (force) {
            nodesTrie.remove(nodeId);
            bucket.decrementNodeCount();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Removed nodeId: " + nodeId 
                        + " from bucket: " + bucket);
            }
            
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Removes the given node from the nodes trie and replaces it with the most recently seen
     * node from the corresponding bucket's replacement cache. If <b>force</b> is set to false, 
     * this method only removes the node if there is a replacement node available.
     * 
     * @param nodeId the node to remove
     * @param force true to remove node in any case, false to remove only if a replacement is available
     * @return true if the node was removed, false otherwise
     */
    protected boolean removeNodeAndReplace(KUID nodeId, boolean force) {
        BucketNode bucket = bucketsTrie.select(nodeId);
        return removeNodeAndReplace(nodeId, bucket, force);
    }
    
    public synchronized void refreshBuckets(boolean force, BootstrapManager manager) throws IOException{
        
        List<KUID> bucketsLookups = new ArrayList<KUID>();
        long now = System.currentTimeMillis();
        
        for(BucketNode bucket : bucketsTrie.values()) {
            long lastTouch = bucket.getTimeStamp();
            //update bucket if freshness limit has passed
            //OR if it is not full (not complete)
            //OR if there is at least one invalid node inside
            //OR if forced 
            //TODO: maybe relax this a little bit?
            int length = Math.max(0, bucket.getDepth()-1);
            List<ContactNode> liveNodes = nodesTrie.range(bucket.getNodeID(), length, SELECT_ALIVE_CONTACTS);
            
            //if we are bootstrapping, phase 1 allready took care of the local bucket
            if (manager != null 
                    && liveNodes.contains(context.getLocalNode())) {
                continue;
            }
            
            if(force || ((now - lastTouch) > RouteTableSettings.BUCKET_REFRESH_TIME.getValue()) 
                    || (bucket.getNodeCount() < K) 
                    || (liveNodes.size() != bucket.getNodeCount())) {
                //select a random ID with this prefix
                KUID randomID = KUID.createPrefxNodeID(bucket.getNodeID().getBytes(), bucket.getDepth());
                
                if(LOG.isTraceEnabled()) {
                    LOG.trace("Refreshing bucket:" + bucket + " with random ID: "+ randomID);
                }
                
                bucketsLookups.add(randomID);
            }
        }
        
        if (manager != null) {
            manager.setBuckets(bucketsLookups);
        }
        
        for(KUID bucketId : bucketsLookups) {
            context.lookup(bucketId, manager);
            routingStats.BUCKET_REFRESH_COUNT.incrementStat();
        }
    }

    public synchronized ContactNode get(KUID nodeId, boolean checkAndUpdateCache) {
        ContactNode node = nodesTrie.get(nodeId);
        if (node == null && checkAndUpdateCache) {
            BucketNode bucket = bucketsTrie.select(nodeId);
            node = bucket.getReplacementNode(nodeId);
        }
        return node;
    }

    public synchronized ContactNode get(KUID nodeId) {
        return get(nodeId, false);
    }

    public synchronized List<ContactNode> getAllNodes() {
        return nodesTrie.values();
    }
    
    public synchronized List<ContactNode> getAllNodesMRS() {
        return BucketUtils.sort(nodesTrie.values());
    }
    
    public synchronized List<ContactNode> getMRSNodes(int numNodes) {
        List<ContactNode> nodesList = getAllNodesMRS();
        return nodesList.subList(0, Math.min(nodesList.size(), numNodes));
    }

    public synchronized List<BucketNode> getAllBuckets() {
        return bucketsTrie.values();
    }

    public synchronized boolean containsNode(KUID nodeId) {
        return nodesTrie.containsKey(nodeId);
    }

    public synchronized ContactNode select(KUID nodeId) {
        return nodesTrie.select(nodeId);
    }
    
    /** 
     * Returns a List of ContactNodes sorted by their 
     * closeness to the provided Key. Use BucketList's
     * sort method to sort the Nodes from least-recently 
     * to most-recently seen.
     */
    public synchronized List<Contact> select(KUID nodeId, int count, 
            boolean liveNodes, boolean willContact) {
        //only touch bucket if we know we are going to contact it's nodes
        if(willContact) {
            touchBucket(nodeId);
        }
        
        if (liveNodes) {
            return TrieUtils.select(nodesTrie, nodeId, count, SELECT_ALIVE_CONTACTS);
        } else {
            return TrieUtils.select(nodesTrie, nodeId, count);
        }
    }
    
    public synchronized int size() {
        return nodesTrie.size();
    }
    
    public synchronized boolean isEmpty() {
        return nodesTrie.isEmpty();
    }

    public synchronized int getBucketCount() {
        return bucketsTrie.size();
    }
    
    public synchronized void clear() {
        nodesTrie.clear();
        bucketsTrie.clear();
        
        init(); // init the Bucket Trie!
    }
    
    private void touchBucket(KUID nodeId) {
        //get bucket closest to node
        BucketNode bucket = bucketsTrie.select(nodeId);
        touchBucket(bucket);
    }
    
    private void touchBucket(BucketNode bucket) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("Touching bucket: " + bucket);
        }
        
        bucket.touch();
    }
    
    public synchronized String toString() {
        Collection<BucketNode> bucketsList = getAllBuckets();
        StringBuilder buffer = new StringBuilder("\n");
        buffer.append("-------------\nLocal node:"+context.getLocalNode()+"\n");
        buffer.append("-------------\nBuckets:\n");
        
        int totalNodesInBuckets = 0;
        for(BucketNode bucket : bucketsList) {
            buffer.append(bucket).append("\n");
            totalNodesInBuckets += bucket.getNodeCount();
            Collection<ContactNode> bucketNodes = nodesTrie.range(bucket.getNodeID(), bucket.getDepth()-1);
            buffer.append("\tNodes:"+"\n");
            for(ContactNode node : bucketNodes) {
                buffer.append("\t"+node+"\n");
            }
        }
        
        buffer.append("-------------\n");
        buffer.append("TOTAL BUCKETS: " + bucketsList.size()).append(" NUM. OF NODES: " + totalNodesInBuckets+"\n");
        buffer.append("-------------\n");
        
        Collection<ContactNode> nodesList = getAllNodes();
        buffer.append("-------------\n");
        buffer.append("TOTAL NODES: " + nodesList.size()).append("\n");
        buffer.append("-------------\n");
        
        return buffer.toString();
    }
    
    /**
     * The SpoofChecker makes sure two Nodes cannot have the same
     * NodeID at the same time.
     */
    private class SpoofCheckerImpl implements SpoofChecker {
        
        private ContactNode currentContact;
        private ContactNode newContact;
        
        private boolean replacementNode;
        private BucketNode replacementBucket;
        
        private SpoofCheckerImpl(ContactNode currentContact, ContactNode newContact, 
                boolean replacementNode, BucketNode replacementBucket) {
            
            this.currentContact = currentContact;
            this.newContact = newContact;
            this.replacementNode = replacementNode;
            this.replacementBucket = replacementBucket;
        }
        
        public void response(ResponseMessage response, long time) {
            ContactNode node = response.getContact();
            touchBucket(node.getNodeID());
            
            if (LOG.isWarnEnabled()) {
                LOG.warn("WARNING: " + newContact + " is trying to spoof its NodeID. " 
                        + node + " responded in " + time + " ms");
            }
            
            routingStats.SPOOF_COUNT.incrementStat();
            
            /*
             * Do nothing else! DefaultMessageHandler takes
             * care of everything else!
             */
        }

        public void timeout(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
            
            // The current contact is obviously not responding
            if (LOG.isInfoEnabled()) {
                LOG.info(currentContact + " does not respond! Replacing it with " + newContact);
            }
            
            updateContactInfo(currentContact, newContact, true);
            
            if (replacementNode && replacementBucket != null) {
                // we have found a live contact in the bucket's replacement cache!
                // It's a good time to replace this bucket's dead entry with this node
                pingLeastRecentlySeenNodeInBucket(replacementBucket);
            }
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof SpoofCheckerImpl)) {
                return false;
            }
            
            // This check is technically not necessary. Just think
            // a bit about the PingManager and how it works.
            return currentContact.getNodeID().equals(
                    ((SpoofCheckerImpl)o).currentContact.getNodeID());
        }
    }
}
