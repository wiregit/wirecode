package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.impl.Bucket;
import com.limegroup.mojito.routing.impl.RouteTableImpl;

public class LimeDHTRoutingTable extends RouteTableImpl {
    
    private static final Log LOG = LogFactory.getLog(LimeDHTRoutingTable.class);
    
    private Map<IpPort, KUID> leafDHTNodes = new HashMap<IpPort, KUID>();

    public LimeDHTRoutingTable(Context context) {
        super(context);
    }
    
    /**
     * Adds a DHT leaf node to the dht routing table, setting a very high timestamp
     * to make sure it always gets contacted first for the first hop of lookups
     * 
     * @param node The DHT leaf node to be added
     */
    public void addLeafDHTNode(InetSocketAddress host) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Pinging node: " + host);
        }
        
        try {
            Contact node = context.ping(host).get();
            if(node != null) {
                synchronized (this) {
                    leafDHTNodes.put(new IpPortImpl(host), node.getNodeID());
                    
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
     * Removes this DHT leaf from our routing table.
     * 
     */
    public synchronized void removeLeafDHTNode(String host, int port) {
        try {
            IpPort node = new IpPortImpl(host, port);
            KUID nodeId = leafDHTNodes.remove(node);
            if(nodeId != null) {
                replaceWithMostRecentlySeenCachedContact(nodeId);
            }
        } catch (UnknownHostException ignored) {}
    }
    
    protected synchronized void replaceWithMostRecentlySeenCachedContact(KUID nodeId) {
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
    
    public synchronized Collection<IpPort> getDHTLeaves(){
        return leafDHTNodes.keySet();
    }
}