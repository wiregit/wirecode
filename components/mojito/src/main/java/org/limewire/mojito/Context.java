package org.limewire.mojito;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFutureProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.storage.Database;
import org.limewire.mojito.util.HostFilter;

/**
 * 
 */
public interface Context {

    /**
     * 
     */
    public MessageDispatcher getMessageDispatcher();
    
    /**
     * 
     */
    public HostFilter getHostFilter();
    
    /**
     * 
     */
    public KUID getLocalNodeID();
    
    /**
     * 
     */
    public SocketAddress getContactAddress();
    
    /**
     * 
     */
    public boolean isLocalNodeID(KUID contactId);
    
    /**
     * 
     */
    public boolean isLocalContactAddress(SocketAddress addr);
    
    /**
     * 
     */
    public boolean isLocalNode(Contact contact);
    
    /**
     * 
     */
    public Contact getLocalNode();
    
    /**
     * 
     */
    public int getExternalPort();
    
    /**
     * 
     */
    public boolean isFirewalled();
    
    /**
     * 
     */
    public MessageHelper getMessageHelper();
    
    /**
     * 
     */
    public MessageFactory getMessageFactory();
    
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
    public boolean isBooting();
    
    /**
     * 
     */
    public boolean isReady();
    
    /**
     * 
     */
    public BigInteger size();
    
    /**
     * 
     */
    public <T> DHTFuture<T> submit(DHTFutureProcess<T> process, 
            long timeout, TimeUnit unit);
}
