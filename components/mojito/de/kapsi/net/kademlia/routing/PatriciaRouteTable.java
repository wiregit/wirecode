/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.routing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.BucketNode;
import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;
import de.kapsi.net.kademlia.event.FindNodeListener;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.settings.NetworkSettings;
import de.kapsi.net.kademlia.settings.RouteTableSettings;
import de.kapsi.net.kademlia.util.BucketUtils;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;
import de.kapsi.net.kademlia.util.NetworkUtils;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class PatriciaRouteTable implements RoutingTable {
    
    private static final Log LOG = LogFactory.getLog(PatriciaRouteTable.class);
    
    private static final int K = KademliaSettings.getReplicationParameter();
    
    private static final int B = RouteTableSettings.getDepthLimit();
    
    private static final long refreshLimit = RouteTableSettings.getBucketRefreshTime();
    
    private Map loopLock = new FixedSizeHashMap(16);
    
    private Context context;
    
    private PatriciaTrie nodesTrie;
    
    private PatriciaTrie bucketsTrie;

    private BucketNode smallestSubtreeBucket;
    
    private int consecutiveFailures = 0;
    
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
    
    public boolean add(ContactNode node, boolean knowToBeAlive) {
        return put(node.getNodeID(), node,knowToBeAlive);
    }
    
    /**
     * @param nodeId
     * @param node
     * @param knowToBeAlive
     * @return true if the node was added, false otherwise
     */
    private boolean put(KUID nodeId, ContactNode node, boolean knowToBeAlive) {
        boolean pingNode = false;
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
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Trying to add "+(knowToBeAlive?"live":"unknown")+" node: "+node+" to routing table");
        }
        //reset the consecutive failure counter
        consecutiveFailures = 0;
        
        // Update an existing node
        ContactNode existingNode = updateExistingNode(nodeId,node,knowToBeAlive);
        if(existingNode != null && (existingNode.getTimeStamp() == 0L || existingNode.isDead())) { 
            //node existed but we don't kow if it's alive
            pingNode = true;
            
        } else if(existingNode == null) {
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
                    touchBucket(nodeId);
                } else {
                    pingNode = true;
                }
                nodesTrie.put(nodeId,node);
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
                //not splitting --> replacement cache. Also a good time to replace stale nodes
                else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("NOT splitting bucket "+ bucket+", adding node "+node+" to replacement cache");
                    }
                    pingNode = addReplacementNode(bucket,node);
                    replaceBucketStaleNodes(bucket);
                }
            }
        }
        if(!knowToBeAlive && pingNode && !node.equals(context.getLocalNode())) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Pinging node: "+node);
            }
            
            PingRequest ping = context.getMessageFactory().createPingRequest();
            try {
                //will get handled by DefaultMessageHandler
                context.getMessageDispatcher().send(node, ping, null);
            } catch (IOException e) {}
        }
        return added;
    }
    
    public void handleFailure(KUID nodeId) {
        
        if(nodeId == null) {
            return;
        }
        //ignore failure if we start getting to many disconnections in a row
        ++consecutiveFailures;
        if(consecutiveFailures > RouteTableSettings.getMaxConsecutiveFailures()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Ignoring node failure as it appears that we are disconnected");
            }
            return;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Handling failure for nodeId: "+nodeId);
        }
        
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
            
            //TODO: we delete dead contacts immediately for now!...maybe relax?
            if(handleNodeFailure(node)) {
                removeNodeAndReplace(nodeId,bucket,true);
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
            if(node!=null) {
                if(!node.failure()) {
                    bucket.addReplacementNode(node);
                }
            }
            if (LOG.isTraceEnabled() && node!= null) {
                LOG.trace("Removed node: "+node+" from replacement cache");
            }
        }
    }
    
    private void replaceBucketStaleNodes(BucketNode bucket) {
        int length = Math.max(0, bucket.getDepth()-1);
        List failingNodes = nodesTrie.range(bucket.getNodeID(),length,new PatriciaTrie.KeySelector(){
            public boolean allow(Object key, Object value) {
                if(value instanceof ContactNode) {
                    return ((ContactNode)value).hasFailed();
                } else {
                    return false;
                }
            }
        });
        for (Iterator iter = failingNodes.iterator(); iter.hasNext();) {
            ContactNode failingNode = (ContactNode) iter.next();
            if(!removeNodeAndReplace(failingNode.getNodeID(),bucket,false)) return;
        }
    }
    
    private boolean removeNodeAndReplace(KUID nodeId, BucketNode bucket,boolean force) {
        ContactNode replacement = bucket.getMostRecentlySeenCachedNode(true);
        if(replacement != null) {
            nodesTrie.remove(nodeId);
            bucket.decrementNodeCount();
            put(replacement.getNodeID(),replacement,false);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Replaced nodeId: "+nodeId+" with node "+ replacement +" from bucket: "+bucket);
            }
            touchBucket(bucket);
            return true;
        } else if(force){
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
     * @return true if the node has been added to the replacement cache, false otherwise
     */
    public boolean addReplacementNode(BucketNode bucket,ContactNode node) {
        
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
            return true;
        } else {
            return false;
        }
    }
    
    private void pingBucketLastRecentlySeenNode(BucketNode bucket) {
        
        if(bucket == null) {
            return;
        }
        
        List bucketList = nodesTrie.range(bucket.getNodeID(), bucket.getDepth()-1);
        
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
    public ContactNode updateExistingNode(KUID nodeId, ContactNode node, boolean alive) {
        boolean replacement = false;
        BucketNode bucket = null;
        
        // Check the RouteTable for existence
        ContactNode existingNode = (ContactNode) nodesTrie.get(nodeId);
        if(existingNode == null) {
            // check replacement cache in closest bucket
            bucket = (BucketNode)bucketsTrie.select(nodeId);
            Map replacementCache = bucket.getReplacementCache();
            
            if (replacementCache!= null && !replacementCache.isEmpty()) {
                existingNode = (ContactNode) replacementCache.get(nodeId);
                
                // If it was neither in the RouteTable nor in the
                // replacement cache then it's new and unknown! We 
                // have to add it first!
                if (existingNode == null) {
                    return null;
                }
                
                replacement = true;
            } else {
                return null;
            }
        }
        //if we are here -> the node is already in the routing table
        if(alive) {
            //if the existing node is marked as dead, replace anyway
            if(existingNode.isDead()) {
                existingNode.setSocketAddress(node.getSocketAddress());
                existingNode.alive();
                touchBucket(nodeId);
                return existingNode;
            }
            
            // Same Address? OK, update timestamp
            InetSocketAddress newAddress = (InetSocketAddress) node.getSocketAddress();
            InetSocketAddress oldAddress = (InetSocketAddress) existingNode.getSocketAddress();
            if (oldAddress.equals(newAddress)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Updating timestamp for node: "+existingNode);
                }
                existingNode.alive();
                touchBucket(nodeId);
                return existingNode;
            }
            //check if we have heard of the existing node recently
            long now = System.currentTimeMillis();
            long delay = now - existingNode.getTimeStamp();
            if(delay < RouteTableSettings.getMinReconnectionTime()) {
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
                if (NetworkUtils.isLocalAddress(newAddress)
                        && NetworkUtils.isLocalAddress(oldAddress)
                        && newAddress.getPort()==oldAddress.getPort()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Local maching loop detection for node: "+existingNode);
                    }
                    existingNode.setSocketAddress(newAddress);
                    existingNode.alive();
                    return existingNode;
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
            ResponseHandler handler 
                = new SpoofCheckHandler(context, 
                    existingNode, node, replacement, bucket);
            
            doSpoofCheck(existingNode, handler);
        } else if(existingNode.isDead()) { //replace anyway and put in unknown state
            existingNode.setSocketAddress(node.getSocketAddress());
            existingNode.setUnknown();
        }
        return existingNode;
    }
    
    private void doSpoofCheck(ContactNode contact, ResponseHandler handler) {
        RequestMessage request = context.getMessageFactory().createPingRequest();
        try {
            context.getMessageDispatcher().send(contact, request, handler);
            loopLock.put(contact.getNodeID(), handler);
        } catch (IOException e) {
            LOG.error("Coud not start spoof check", e);
        }
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
            //TODO: maybe relax this a little bit?
            int length = Math.max(0, bucket.getDepth()-1);
            List liveNodes = nodesTrie.range(bucket.getNodeID(), length, new NodeAliveKeySelector());
            
            if(force || ((now - lastTouch) > refreshLimit) 
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
        if(bucketsLookups.size() == 0) {
            l.secondPhaseComplete(0,false);
            return;
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
        ContactNode node = (ContactNode)nodesTrie.get(nodeId);
        if (node == null && checkAndUpdateCache) {
            BucketNode bucket = (BucketNode)bucketsTrie.select(nodeId);
            node = (ContactNode)bucket.getReplacementNode(nodeId);
        }
        return node;
    }

    public ContactNode get(KUID nodeId) {
        return get(nodeId, false);
    }

    public List getAllNodes() {
        return nodesTrie.values();
    }

    public List getAllBuckets() {
        return bucketsTrie.values();
    }

    /**
     * Increments ContactNode's failure counter, marks it as stale
     * if a certain error level is exceeded and returns 
     * true if it's the case.
     */
    private boolean handleNodeFailure(ContactNode node) {
        if (node != null) {
            //node has been alive at least once
            return node.failure();
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
        if(isLocalLookup) {
            touchBucket(lookup);
        }
        
        if(onlyLiveNodes) {
            return nodesTrie.select(lookup, k, new NodeAliveKeySelector());
        } else {
            return nodesTrie.select(lookup, k);
        }
    }

    // TODO: remove since unused
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
    
    public String toString() {
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
    
    private class BootstrapFindNodeListener implements FindNodeListener{

        private List queryList;
        
        private BootstrapListener bootstrapListener;
        
        private boolean foundNodes;
        
        private BootstrapFindNodeListener(List queryList,BootstrapListener listener) {
            this.queryList = queryList;
            this.bootstrapListener = listener;
        }
        
        public synchronized void foundNodes(KUID lookup, Collection nodes, Map queryKeys, long time) {
            if(!foundNodes && !nodes.isEmpty()) {
                foundNodes = true;
            }
            queryList.remove(lookup);
            if(queryList.isEmpty() 
                    && bootstrapListener != null) {
                bootstrapListener.secondPhaseComplete(time,foundNodes);
            }
        }
    }
    
    private class NodeAliveKeySelector implements PatriciaTrie.KeySelector{
        public boolean allow(Object key, Object value) {
            if(value instanceof ContactNode) {
                return !((ContactNode)value).hasFailed();
            } else {
                return false;
            }
        }
    }
    
    /**
     * Handles a spoof check where we're trying to figure out
     * wheather or not a Node is trying to spoof its Node ID.
     */
    private class SpoofCheckHandler extends AbstractResponseHandler {
        
        private boolean done = false;
        
        private int errors = 0;
        
        private ContactNode currentContact;
        private ContactNode newContact;
        
        private boolean replacementNode;
        private BucketNode replacementBucket;
        
        public SpoofCheckHandler(Context context, ContactNode currentContact, ContactNode newContact, 
                boolean replacementNode, BucketNode replacementBucket) {
            super(context);
            
            this.currentContact = currentContact;
            this.newContact = newContact;
            this.replacementNode = replacementNode;
            this.replacementBucket = replacementBucket;
        }

        public void handleResponse(KUID nodeId, SocketAddress src, 
                Message message, long time) throws IOException {
            
            if (done) {
                return;
            }
            
            loopLock.remove(nodeId);
            done = true;
            touchBucket(nodeId);
            if (LOG.isWarnEnabled()) {
                LOG.warn("WARNING: "+newContact + " is trying to spoof its NodeID. " 
                        + ContactNode.toString(nodeId, src) 
                        + " responded in " + time + " ms");
            }
            
            // Do nothing else! DefaultMessageHandler takes
            // care of everything else!
            //TODO: add bad node to IP Filter
        }

        public void handleTimeout(KUID nodeId, SocketAddress dst, 
                long time) throws IOException {
            
            if (done) {
                return;
            }
            
            // Try at least x-times before giving up!
            if (++errors >= NetworkSettings.getMaxErrors()) {
                
                loopLock.remove(nodeId);
                done = true;
                
                // The current contact is obviously not responding
                if (LOG.isInfoEnabled()) {
                    LOG.info(currentContact + " does not respond! Replacing it with " + newContact);
                }
                currentContact.setSocketAddress(newContact.getSocketAddress());
                currentContact.alive();
                if(replacementNode && replacementBucket!=null) {
                    // we have found a live contact in the bucket's replacement cache!
                    // It's a good time to replace this bucket's dead entry with this node
                    pingBucketLastRecentlySeenNode(replacementBucket);
                }
                return;
            }
                
            doSpoofCheck(currentContact, this);
        }
    }
}
