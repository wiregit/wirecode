package org.limewire.mojito;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.SecurityTokenEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.LocalContact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.storage.Value;

/**
 * 
 */
public interface MojitoDHT extends DHT {
    
    /**
     * Returns the localhost's {@link Contact}.
     */
    @Override
    public LocalContact getLocalhost();
    
    /**
     * Returns {@code true} if the given {@link Contact} is equal
     * to the localhost's {@link Contact}.
     */
    public boolean isLocalhost(Contact contact);
    
    /**
     * Returns the {@link Vendor}.
     */
    public Vendor getVendor();
    
    /**
     * Returns the {@link Version}.
     */
    public Version getVersion();
    
    /**
     * Returns the localhost's {@link KUID}.
     */
    public KUID getContactId();
    
    /**
     * Sets the localhost's {@link KUID}.
     */
    public void setContactId(KUID contactId);
    
    /**
     * Returns the localhost's {@link SocketAddress}.
     */
    public SocketAddress getContactAddress();
    
    /**
     * Sets the localhost's {@link SocketAddress}.
     */
    public void setContactAddress(SocketAddress address);
    
    /**
     * Sets the localhost's external {@link SocketAddress}.
     */
    public void setExternalAddress(SocketAddress address);
    
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
    public DHTFuture<StoreEntity> put(KUID key, Value value);
    
    /**
     * 
     */
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value);
 
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
    public DHTFuture<ValueEntity> get(ValueKey key);
    
    /**
     * 
     */
    public DHTFuture<ValueEntity[]> getAll(ValueKey key);
    
    /**
     * 
     */
    public DHTFuture<SecurityTokenEntity> getSecurityToken(
            Contact dst, long timeout, TimeUnit unit);
}
