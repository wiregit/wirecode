package org.limewire.mojito2;

import java.io.Closeable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModelManager;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.util.HostFilter;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.message.MessageFactory;

/**
 * 
 */
public interface DHT extends Closeable {

    /**
     * 
     */
    public String getName();
    
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
    public Contact getLocalNode();
    
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
    public MessageDispatcher getMessageDispatcher();
    
    /**
     * 
     */
    public MessageFactory getMessageFactory();
    
    /**
     * 
     */
    public HostFilter getHostFilter();
    
    /**
     * 
     */
    public void setHostFilter(HostFilter hostFilter);
    
    /**
     * 
     */
    public StorableModelManager getStorableModelManager();
    
    /**
     * 
     */
    public void start();
    
    /**
     * 
     */
    public void stop();
    
    /**
     * 
     */
    public DHTFuture<BootstrapEntity> bootstrap(Contact dst, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(String address, int port, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(InetAddress address, int port, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(SocketAddress dst, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(Contact dst, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            Contact[] dst, long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<ValueEntity> get(KUID lookupId, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<ValueEntity> get(EntityKey key, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<StoreEntity> put(Storable storable, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<StoreEntity> put(DHTValueEntity value, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public <T> DHTFuture<T> submit(AsyncProcess<T> process, 
            long timeout, TimeUnit unit);
}
