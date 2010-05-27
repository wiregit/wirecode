package org.limewire.mojito;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.AsyncProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.storage.DHTValue;
import org.limewire.mojito.storage.DHTValueEntity;
import org.limewire.mojito.storage.Database;
import org.limewire.mojito.util.HostFilter;

/**
 * 
 */
public interface DHT extends ContactPinger, AddressPinger, Closeable {
    
    /**
     * Returns the name of the {@link DHT} instance.
     */
    public String getName();
    
    /**
     * Returns the size of the {@link DHT}
     */
    public BigInteger size();
    
    /**
     * Returns {@code true} if the DHT is firewalled
     */
    public boolean isFirewalled();
    
    /**
     * Returns the localhost {@link Contact}
     */
    public Contact getLocalNode();
    
    /**
     * Returns the {@link RouteTable}.
     */
    public RouteTable getRouteTable();
    
    /**
     * Returns the {@link Database}.
     */
    public Database getDatabase();
    
    /**
     * Returns the {@link MessageDispatcher}.
     */
    public MessageDispatcher getMessageDispatcher();
    
    /**
     * Returns the {@link MessageFactory}.
     */
    public MessageFactory getMessageFactory();
    
    /**
     * Returns the {@link HostFilter}.
     */
    public HostFilter getHostFilter();
    
    /**
     * Sets the {@link HostFilter}.
     */
    public void setHostFilter(HostFilter hostFilter);
    
    /**
     * Binds the {@link DHT} to the given {@link Transport}.
     */
    public void bind(Transport transport) throws IOException;
    
    /**
     * Unbinds the {@link DHT} from the {@link Transport} and returns it.
     */
    public Transport unbind();
    
    /**
     * Returns {@code true} if the {@link DHT} is bound to a {@link Transport}
     */
    public boolean isBound();
    
    /**
     * Returns true if the {@link DHT} is in the process of bootstrapping
     */
    public boolean isBooting();
    
    /**
     * Returns true if the {@link DHT} is ready (i.e. bootstrapped)
     */
    public boolean isReady();
    
    /**
     * 
     */
    public DHTFuture<BootstrapEntity> bootstrap(Contact dst, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress dst, 
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
    public DHTFuture<PingEntity> ping(Contact src, 
            Contact[] dst, long timeout, TimeUnit unit);
    
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
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value, 
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
