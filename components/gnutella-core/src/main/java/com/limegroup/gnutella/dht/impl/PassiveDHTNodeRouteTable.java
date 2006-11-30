package com.limegroup.gnutella.dht.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.MojitoDHT;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.concurrent.DHTFutureListener;
import com.limegroup.mojito.result.PingResult;
import com.limegroup.mojito.routing.Bucket;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.impl.RouteTableImpl;

/**
 * Passive Nodes (Ultrapeers) use this slightly extended version 
 * of the RouteTable. It maintains an internal mapping of DHT enabled 
 * leaves that are currently connected to the Ultrapeer (Gnutella 
 * connections). 
 */
class PassiveDHTNodeRouteTable extends RouteTableImpl {
    
    private static final long serialVersionUID = 707016966528414433L;

    private static final Log LOG = LogFactory.getLog(PassiveDHTNodeRouteTable.class);
    
    /**
     * The Map storing the leaf nodes connected to this ultrapeer. The mapping is
     * used to go from Gnutella <tt>IpPort</tt> to DHT <tt>RemoteContact</tt>. 
     */
    private Map<SocketAddress, KUID> leafDHTNodes = new HashMap<SocketAddress, KUID>();

    private MojitoDHT dht;
    
    public PassiveDHTNodeRouteTable(MojitoDHT dht) {
        this.dht = dht;
    }
    
    /**
     * Adds a DHT leaf node to the dht routing table, setting a very high timestamp
     * to make sure it always gets contacted first for the first hop of lookups, and 
     * make sure it always gets returned first when selecting the MRS nodes from the RT.
     * 
     * @param node The DHT leaf node to be added
     */
    public void addLeafDHTNode(String host, int port) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Pinging leaf: " + host + ": " + port);
        }
        
        final InetSocketAddress addr = new InetSocketAddress(host, port);
        DHTFuture<PingResult> future = dht.ping(addr);
        
        DHTFutureListener<PingResult> listener = new DHTFutureListener<PingResult>() {
            public void futureDone(DHTFuture<? extends PingResult> future) {
                try {
                    PingResult result = future.get();
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Ping succeeded to: " + result);
                    }
                    
                    Contact node = result.getContact();
                    synchronized (PassiveDHTNodeRouteTable.this) {
                        KUID previous = leafDHTNodes.put(addr, node.getNodeID());
                        
                        if (previous == null || !previous.equals(node.getNodeID())) {
                            // Add it as a priority Node
                            node.setTimeStamp(Contact.PRIORITY_CONTACT);
                            add(node);
                        }
                    }
                } catch (ExecutionException e) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Ping failed to: " + addr, e);
                    }
                } catch (CancellationException ignore) {
                } catch (InterruptedException ignore) {
                }
            }
        };
        
        future.addDHTFutureListener(listener);
    }
    
    /**
     * Removes this DHT leaf from our routing table and returns it.
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
    
    /**
     * Removes the given node from the routing table and tries to replace
     * it with the Most Recently Seen cached contact.
     * 
     * @param nodeId The nodeId of the node to remove
     */
    private synchronized void removeAndReplaceWithMRSCachedContact(KUID nodeId) {
        Bucket bucket = getBucket(nodeId);
        boolean removed = bucket.remove(nodeId);

        if(removed) {
            Contact mrs = bucket.getMostRecentlySeenCachedContact();
            if (mrs != null) {
                removed = bucket.removeCachedContact(mrs.getNodeID());
                assert (removed == true);
                
                bucket.addActiveContact(mrs);
            }
        }
    }
    
    /**
     * Returns whether or not this Ultrapeer has any
     * DHT enabled leaves
     */
    public synchronized boolean hasDHTLeaves() {
        return !leafDHTNodes.isEmpty();
    }
    
    /**
     * Returns the IP:Ports of this Ultrapeer's DHT enabled leaves
     * 
     * Hold a lock on 'this' when using the Iterator! 
     */
    public synchronized Set<SocketAddress> getDHTLeaves() {
        return Collections.unmodifiableSet(leafDHTNodes.keySet());
    }
}