package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.event.PingListener;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.routing.impl.Bucket;
import com.limegroup.mojito.routing.impl.RouteTableImpl;
import com.limegroup.mojito.settings.ContextSettings;

public class LimeDHTRoutingTable extends RouteTableImpl {
    
    private static final Log LOG = LogFactory.getLog(LimeDHTRoutingTable.class);
    
    private Map<IpPortImpl, KUID> leafDHTNodes = new HashMap<IpPortImpl, KUID>();

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
        
        final Contact[] dhtNode = new Contact[] { null };
        synchronized (dhtNode) {
                try {
                    context.ping(host, new PingListener() {
                        public void response(ResponseMessage response, long t) {
                            dhtNode[0] = response.getContact();
                            synchronized (dhtNode) {
                                dhtNode.notify();
                            }
                        }

                        public void timeout(KUID nodeId, SocketAddress address, 
                                RequestMessage request, long t) {
                            synchronized (dhtNode) {
                                dhtNode.notify();
                            }
                        }
                    });
                } catch (IOException ignored) {}
                
            try {
                dhtNode.wait(ContextSettings.SYNC_PING_TIMEOUT.getValue());
            } catch (InterruptedException err) {
                LOG.error("InterruptedException", err);
            }
        }
        
        Contact node = dhtNode[0];
        if(node != null) {
            synchronized (this) {
                leafDHTNodes.put(new IpPortImpl(host), node.getNodeID());
                
                node.setState(State.ALIVE);
                node.setTimeStamp(Long.MAX_VALUE);
                add(node);
            }
        }
    }
    
    /**
     * Removes this DHT leaf from our routing table.
     * 
     */
    public synchronized void removeLeafDHTNode(String host, int port) {
        try {
            IpPortImpl node = new IpPortImpl(host, port);
            KUID nodeId = leafDHTNodes.remove(node);
            if(nodeId != null) {
                replaceWithMostRecentlySeenCachedContact(nodeId);
            }
        } catch (UnknownHostException ignored) {}
    }
    
    protected synchronized void replaceWithMostRecentlySeenCachedContact(KUID nodeId) {
        Bucket bucket = bucket(nodeId);
        boolean removed = bucket.remove(nodeId);
        assert (removed == true);
        
        Contact mrs = bucket.getMostRecentlySeenCachedContact();
        if (mrs != null) {
            removed = bucket.removeCachedContact(mrs.getNodeID());
            assert (removed == true);
            
            bucket.addLiveContact(mrs);
        }
    }
}
