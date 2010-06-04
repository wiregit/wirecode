package org.limewire.mojito;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureProcess;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.storage.Database;
import org.limewire.mojito.util.HostFilter;

/**
 * The {@link Context} provides some glue code for the Mojito 
 * front- and backend.
 */
public interface Context {

    /**
     * Returns the {@link MessageDispatcher}.
     */
    public MessageDispatcher getMessageDispatcher();
    
    /**
     * Returns the {@link HostFilter}.
     */
    public HostFilter getHostFilter();
    
    /**
     * Returns the localhost's {@link KUID}.
     */
    public KUID getLocalNodeID();
    
    /**
     * Returns the localhost's {@link SocketAddress}.
     */
    public SocketAddress getContactAddress();
    
    /**
     * Returns {@code true} if the given {@link KUID} is equals to
     * the localhost's {@link KUID}.
     */
    public boolean isLocalNodeID(KUID contactId);
    
    /**
     * Returns {@code true} if the given {@link SocketAddress} is
     * equals to the localhost's {@link SocketAddress}.
     */
    public boolean isLocalContactAddress(SocketAddress addr);
    
    /**
     * Returns {@code true} if the given {@link Contact} is equal
     * to the localhost's {@link Contact}.
     */
    public boolean isLocalNode(Contact contact);
    
    /**
     * Returns the localhost's {@link Contact}.
     */
    public Contact getLocalNode();
    
    /**
     * Returns the localhost's external port number.
     */
    public int getExternalPort();
    
    /**
     * Returns true if the localhost is firewalled
     */
    public boolean isFirewalled();
    
    /**
     * Returns the {@link MessageHelper}.
     */
    public MessageHelper getMessageHelper();
    
    /**
     * Returns the {@link MessageFactory}.
     */
    public MessageFactory getMessageFactory();
    
    /**
     * Returns the {@link RouteTable}.
     */
    public RouteTable getRouteTable();
    
    /**
     * Returns the {@link Database}.
     */
    public Database getDatabase();
    
    /**
     * Returns {@code true} if the localhost is booting.
     */
    public boolean isBooting();
    
    /**
     * Returns {@code true} if the localhost is ready.
     */
    public boolean isReady();
    
    /**
     * Returns the approximate size of the DHT.
     */
    public BigInteger size();
    
    /**
     * Submits the given {@link DHTFutureProcess} for execution.
     */
    public <T> DHTFuture<T> submit(DHTFutureProcess<T> process, 
            long timeout, TimeUnit unit);
}
