package org.limewire.mojito;

import java.io.Closeable;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.MessageDispatcher2;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.util.HostFilter;

/**
 * 
 */
public interface MojitoDHT2 extends Closeable {

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
    public MessageDispatcher2 getMessageDispatcher();
    
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
    public DHTFuture<PingEntity> ping(SocketAddress dst, 
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(Contact dst, long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            long timeout, TimeUnit unit);
    
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
}
