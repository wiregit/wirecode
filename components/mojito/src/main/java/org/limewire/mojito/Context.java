package org.limewire.mojito;

import java.math.BigInteger;

import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.LocalContact;
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
     * Returns the localhost's {@link Contact}.
     */
    public LocalContact getLocalhost();
    
    /**
     * Returns the {@link MessageHelper}.
     */
    public MessageHelper getMessageHelper();
    
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
}
