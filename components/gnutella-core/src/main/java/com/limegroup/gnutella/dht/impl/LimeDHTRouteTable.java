package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.DHTFuture;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.event.DHTEventListener;
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
            LOG.debug("Pinging leaf: " + host + ": " + port);
        }
        
        final InetSocketAddress addr = new InetSocketAddress(host, port);
        DHTFuture<Contact> future = dht.ping(addr);
        future.addDHTEventListener(new DHTEventListener<Contact>() {
            public void handleResult(Contact node) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Ping succeeded to: " + node);
                }
                
                synchronized (this) {
                    KUID previous = leafDHTNodes.put(addr, node.getNodeID());
                    
                    if(previous == null || !previous.equals(node.getNodeID())) {
                        //add it as a priority node
                        node.setTimeStamp(Long.MAX_VALUE);
                        add(node);
                    }
                }
            }

            public void handleThrowable(Throwable ex) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Ping failed to: " + addr, ex);
                }
            }
            
        });
    }
    
    /**
     * Removes this DHT leaf from our routing table and returns it.
     * 
     */
    public synchronized SocketAddress removeLeafDHTNode(String host, int port) {
        
        SocketAddress addr = new InetSocketAddress(host, port);
        KUID nodeId = leafDHTNodes.remove(addr);
        if(nodeId != null) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Removed leaf: " + host + ": " + port);
            }
            
            removeAndReplaceWithMRSCachedContact(nodeId);
            return addr;
        }
        return null;
    }
    
    private void removeAndReplaceWithMRSCachedContact(KUID nodeId) {
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