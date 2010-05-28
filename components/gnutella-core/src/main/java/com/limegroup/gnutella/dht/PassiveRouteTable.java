package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.AddressPinger;
import org.limewire.mojito.ContactPinger;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTValueFuture;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.RequestTimeoutException;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.ClassfulNetworkCounter;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTableImpl;
import org.limewire.mojito.settings.NetworkSettings;

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
     * The actual RouteTable
     */
    private final RouteTable delegate = new RouteTableImpl();
    
    /**
     * The Map storing the leaf nodes connected to this ultrapeer. The mapping is
     * used to go from Gnutella <tt>IpPort</tt> to DHT <tt>RemoteContact</tt>. 
     */
    private final Map<SocketAddress, KUID> leafDHTNodes 
        = new HashMap<SocketAddress, KUID>();
    
    private volatile AddressPinger pinger;
    
    private DHTFuture<PingEntity> ping(SocketAddress address) {
        AddressPinger pinger = this.pinger;
        
        DHTFuture<PingEntity> future = null;
        if (pinger != null) {
            long timeout = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
            future = pinger.ping(address, timeout, TimeUnit.MILLISECONDS);
        }
        
        if (future != null) {
            return future;
        }
        
        RequestTimeoutException exception 
            = new RequestTimeoutException(
                null, address, 
                0L, TimeUnit.MILLISECONDS);
        
        return new DHTValueFuture<PingEntity>(exception);
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
    public void addLeafNode(final SocketAddress address) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Pinging leaf: " + address);
        }
        
        DHTFuture<PingEntity> future = ping(address);
        
        EventListener<FutureEvent<PingEntity>> listener 
                = new EventListener<FutureEvent<PingEntity>>() {
            @Override
            public void handleEvent(FutureEvent<PingEntity> event) {
                switch (event.getType()) {
                    case SUCCESS:
                        onSuccess(event.getResult());
                        break;
                    case EXCEPTION:
                        onException(event.getException());
                        break;
                }
            }

            private void onSuccess(PingEntity result) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ping succeeded to: " + result);
                }
                
                Contact node = result.getContact();
                synchronized (PassiveRouteTable.this) {
                    KUID previous = leafDHTNodes.put(address, node.getContactId());
                    
                    if (previous == null || !previous.equals(node.getContactId())) {
                        // Add it as a priority Node
                        node.setTimeStamp(Contact.PRIORITY_CONTACT);
                        add(node);
                    }
                }
            }

            private void onException(ExecutionException e) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Ping failed to: " + address, e);
                }
            }
        };
        
        future.addFutureListener(listener);
    }
    
    /**
     * Removes this DHT leaf from our routing table and returns it.
     */
    public synchronized SocketAddress removeLeafNode(SocketAddress address) {
        
        KUID nodeId = leafDHTNodes.remove(address);
        if(nodeId != null) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Removed leaf: " + address);
            }
            
            removeAndReplaceWithMRSCachedContact(nodeId);
            return address;
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
                    removed = bucket.removeCachedContact(mrs.getContactId());
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
        this.pinger = (AddressPinger)pinger;
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
