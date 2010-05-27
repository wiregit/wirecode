package org.limewire.mojito2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.SecurityTokenEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.Database;

/**
 * 
 */
public interface MojitoDHT extends DHT {
    
    /**
     * 
     */
    @Override
    public LocalContact getLocalNode();
    
    /**
     * 
     */
    public boolean isLocalNode(Contact contact);
    
    /**
     * 
     */
    public Vendor getVendor();
    
    /**
     * 
     */
    public Version getVersion();
    
    /**
     * 
     */
    public KUID getLocalNodeID();
    
    /**
     * 
     */
    public void setContactId(KUID contactId);
    
    /**
     * 
     */
    public SocketAddress getContactAddress();
    
    /**
     * 
     */
    public void setContactAddress(SocketAddress address);
    
    /**
     * 
     */
    public void setExternalAddress(SocketAddress address);
    
    /**
     * 
     */
    public void bind(Transport transport) throws IOException;
    
    /**
     * 
     */
    public boolean isBound();
    
    /**
     * 
     */
    public Transport unbind();
    
    /**
     * 
     */
    public RouteTable getRouteTable();
    
    /**
     * 
     */
    public Database getDatabase();
    
    /**
     * 
     */
    public BigInteger size();
    
    /**
     * 
     */
    public boolean isFirewalled();
    
    /**
     * 
     */
    public boolean isReady();

    /**
     * 
     */
    public boolean isBooting();
    
    /**
     * 
     */
    public DHTFuture<PingEntity> findActiveContact();
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(String address, int port);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(InetAddress address, int port);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(SocketAddress addr);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(Contact src, Contact[] dst);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> collisionPing(Contact dst);
    
    /**
     * 
     */
    public DHTFuture<BootstrapEntity> bootstrap(String address, int port);
    
    /**
     * 
     */
    public DHTFuture<BootstrapEntity> bootstrap(InetAddress address, int port);
    
    /**
     * 
     */
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress addr);
    
    /**
     * 
     */
    public DHTFuture<BootstrapEntity> bootstrap(Contact contact);
    
    /**
     * 
     */
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value);
 
    /**
     * 
     */
    public DHTFuture<StoreEntity> remove(KUID key);
    
    /**
     * 
     */
    public DHTFuture<NodeEntity> lookup(KUID lookupId);
    
    /**
     * 
     */
    public DHTFuture<ValueEntity> get(EntityKey key);
    
    /**
     * 
     */
    public DHTFuture<ValueEntity[]> getAll(EntityKey key);
    
    /**
     * 
     */
    public DHTFuture<SecurityTokenEntity> getSecurityToken(
            Contact dst, long timeout, TimeUnit unit);
}
