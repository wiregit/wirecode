package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.routing.impl.Bucket;
import com.limegroup.mojito.routing.impl.RouteTableImpl;

class LimeDHTRouteTable extends RouteTableImpl {
    
    private static final long serialVersionUID = 707016966528414433L;

    private static final Log LOG = LogFactory.getLog(LimeDHTRouteTable.class);
    
    private Map<SocketAddress, KUID> leafDHTNodes = new HashMap<SocketAddress, KUID>();

    private MojitoDHT dht;
    
    public LimeDHTRouteTable(MojitoDHT dht) {
        this.dht = dht;
    }
    
    /**
     * Adds a DHT leaf node to the dht routing table, setting a very high timestamp
     * to make sure it always gets contacted first for the first hop of lookups
     * 
     * @param node The DHT leaf node to be added
     */
    public void addLeafDHTNode(String host, int port) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Pinging node: " + host);
        }
        
        try {
            InetSocketAddress addr = new InetSocketAddress(host, port);
            Contact node = dht.ping(addr).get();
            if(node != null) {
                synchronized (this) {
                    leafDHTNodes.put(addr, node.getNodeID());
                    
                    node.setTimeStamp(Long.MAX_VALUE);
                    add(node);
                }
            }
        } catch (InterruptedException err) {
            LOG.error("InterruptedException", err);
        } catch (ExecutionException err) {
            LOG.error("ExecutionException", err);
        } 
    }
    
    /**
     * Removes this DHT leaf from our routing table and returns it.
     * 
     */
    public synchronized SocketAddress removeLeafDHTNode(String host, int port) {
        SocketAddress addr = new InetSocketAddress(host, port);
        KUID nodeId = leafDHTNodes.remove(addr);
        if(nodeId != null) {
            removeAndReplaceWithMRSCachedContact(nodeId);
            return addr;
        }
        return null;
    }
    
    protected synchronized void removeAndReplaceWithMRSCachedContact(KUID nodeId) {
        Bucket bucket = getBucket(nodeId);
        boolean removed = bucket.remove(nodeId);
        assert (removed == true);
        
        Contact mrs = bucket.getMostRecentlySeenCachedContact();
        if (mrs != null) {
            removed = bucket.removeCachedContact(mrs.getNodeID());
            assert (removed == true);
            
            bucket.addLiveContact(mrs);
        }
    }
    
    public boolean hasDHTLeaves() {
        return !leafDHTNodes.isEmpty();
    }
    
    public synchronized Collection<SocketAddress> getDHTLeaves(){
        return leafDHTNodes.keySet();
    }
}