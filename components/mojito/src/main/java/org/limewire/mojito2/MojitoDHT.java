package org.limewire.mojito2;

import java.net.InetAddress;
import java.net.SocketAddress;

import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.Storable;

/**
 * 
 */
public interface MojitoDHT extends DHT {

    /**
     * 
     */
    public Context getContext();
    
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
    public DHTFuture<StoreEntity> store(Storable storable);
    
    /**
     * 
     */
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value);
 
    /**
     * 
     */
    public DHTFuture<ValueEntity> get(EntityKey key);
    
    /**
     * 
     */
    public DHTFuture<ValueEntity[]> getAll(EntityKey key);
}
