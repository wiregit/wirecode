package com.limegroup.gnutella.dht2;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.ContactPinger;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.routing.Bucket;
import org.limewire.mojito2.routing.ClassfulNetworkCounter;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;

/**
 * Passive Nodes (Ultrapeers) use this slightly extended version 
 * of the <code>RouteTable</code>. It maintains an internal mapping of DHT 
 * enabled leaves that are currently connected to the Ultrapeer (Gnutella 
 * connections). 
 */
@SuppressWarnings("serial")
public class PassiveRouteTable implements RouteTable {
    
    private static final Log LOG = LogFactory.getLog(PassiveRouteTable.class);
    
    /**
     * MojitoDHT instance
     */
    private final MojitoDHT dht;
    
    /**
     * The actual RouteTable
     */
    private final RouteTable delegate;
    
    /**
     * The Map storing the leaf nodes connected to this ultrapeer. The mapping is
     * used to go from Gnutella <tt>IpPort</tt> to DHT <tt>RemoteContact</tt>. 
     */
    private final Map<SocketAddress, KUID> leafDHTNodes = new HashMap<SocketAddress, KUID>();
    
    public PassiveRouteTable(MojitoDHT dht) {
        assert (dht.isFirewalled()); // Must be firewalled
        
        this.dht = dht;
        delegate = dht.getRouteTable();
    }

    /**
     * Adds a DHT leaf node to the DHT routing table, setting a very high timestamp
     * to make sure it always gets contacted first for the first hop of lookups, and 
     * make sure it always gets returned first when selecting the Most Recently Seen
     * (MRS) nodes from the RT.
     * 
     * @param host the IP address of the remote host as a string
     * @param port the listening port for the remote host
     */
    public void addLeafDHTNode(String host, int port) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Pinging leaf: " + host + ": " + port);
        }
        
        final InetSocketAddress addr = new InetSocketAddress(host, port);
        DHTFuture<PingEntity> future = dht.ping(addr);
        
        EventListener<FutureEvent<PingEntity>> listener 
                = new EventListener<FutureEvent<PingEntity>>() {
            @Override
            public void handleEvent(FutureEvent<PingEntity> event) {
                switch (event.getType()) {
                    case SUCCESS:
                        handleFutureSuccess(event.getResult());
                        break;
                    case EXCEPTION:
                        handleExecutionException(event.getException());
                        break;
                }
            }

            private void handleFutureSuccess(PingEntity result) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Ping succeeded to: " + result);
                }
                
                Contact node = result.getContact();
                synchronized (PassiveRouteTable.this) {
                    KUID previous = leafDHTNodes.put(addr, node.getNodeID());
                    
                    if (previous == null || !previous.equals(node.getNodeID())) {
                        // Add it as a priority Node
                        node.setTimeStamp(Contact.PRIORITY_CONTACT);
                        add(node);
                    }
                }
            }

            private void handleExecutionException(ExecutionException e) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Ping failed to: " + addr, e);
                }
            }
        };
        
        future.addFutureListener(listener);
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
     * it with the Most Recently Seen (MRS) cached contact.
     * 
     * @param nodeId the KUID of the node to remove
     */
    private synchronized void removeAndReplaceWithMRSCachedContact(KUID nodeId) {
        Bucket bucket = getBucket(nodeId);
        boolean removed = bucket.removeActiveContact(nodeId);

        if (removed) {
            if (bucket.getCacheSize() > 0) {
                ClassfulNetworkCounter counter = bucket.getClassfulNetworkCounter();
                
                Contact mrs = null;
                while((mrs = bucket.getMostRecentlySeenCachedContact()) != null) {
                    removed = bucket.removeCachedContact(mrs.getNodeID());
                    assert (removed == true);
                    
                    if (counter == null || counter.isOkayToAdd(mrs)) {
                        bucket.addActiveContact(mrs);
                        break;
                    }
                }
            }
        } else {
            bucket.removeCachedContact(nodeId);
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
    
    // --- ROUTE TABLE ---
    
    @Override
    public synchronized void add(Contact node) {
        delegate.add(node);
    }

    @Override
    public synchronized void addRouteTableListener(RouteTableListener l) {
        delegate.addRouteTableListener(l);
    }

    @Override
    public synchronized void removeRouteTableListener(RouteTableListener l) {
        delegate.removeRouteTableListener(l);
    }
    
    @Override
    public synchronized Contact get(KUID nodeId) {
        return delegate.get(nodeId);
    }

    @Override
    public synchronized Collection<Contact> getActiveContacts() {
        return delegate.getActiveContacts();
    }

    @Override
    public synchronized Bucket getBucket(KUID nodeId) {
        return delegate.getBucket(nodeId);
    }

    @Override
    public synchronized Collection<Bucket> getBuckets() {
        return delegate.getBuckets();
    }

    @Override
    public synchronized Collection<Contact> getCachedContacts() {
        return delegate.getCachedContacts();
    }

    @Override
    public synchronized Collection<Contact> getContacts() {
        return delegate.getContacts();
    }

    @Override
    public synchronized Contact getLocalNode() {
        return delegate.getLocalNode();
    }

    @Override
    public synchronized Collection<KUID> getRefreshIDs(boolean bootstrapping) {
        return delegate.getRefreshIDs(bootstrapping);
    }

    @Override
    public synchronized void handleFailure(KUID nodeId, SocketAddress address) {
        delegate.handleFailure(nodeId, address);
    }

    @Override
    public synchronized boolean isLocalNode(Contact node) {
        return delegate.isLocalNode(node);
    }

    @Override
    public synchronized void purge(long elapsedTimeSinceLastContact) {
        delegate.purge(elapsedTimeSinceLastContact);
    }

    @Override
    public synchronized void purge(PurgeMode first, PurgeMode... rest) {
        delegate.purge(first, rest);
    }
    
    @Override
    public synchronized Contact select(KUID nodeId) {
        return delegate.select(nodeId);
    }
    
    @Override
    public synchronized Collection<Contact> select(KUID nodeId, int count, SelectMode mode) {
        return delegate.select(nodeId, count, mode);
    }
    
    @Override
    public synchronized void bind(ContactPinger pinger) {
        delegate.bind(pinger);
    }
    
    @Override
    public synchronized int size() {
        return delegate.size();
    }
    
    @Override
    public synchronized void clear() {
        delegate.clear();
    }
    
    @Override
    public synchronized String toString() {
        return "Passive RouteTable: " + delegate.toString();
    }
}
