package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.Network;
import org.limewire.listener.EventListener;

/**
 * Allows the xmpp service user to provide configuration for xmpp login.
 */
public interface XMPPConnectionConfiguration extends Network {
    
    /** Returns true if the account should enable debugging. (This causes a smack debug window to appear.) */
    public boolean isDebugEnabled();
    
    /** Returns the username this configuration will use. */
    public String getUsername();
    
    /** Returns the password this configuration will use. */
    public String getPassword();
    
    /** Returns the host this configuration will connect to. */
    public String getHost();
    
    /** Returns the port this configuration will connect to. */
    public int getPort();
    
    /**
     * Returns a user-friendly name for the account.
     * This is something like <code>Example</code> for users of example.com's
     * XMPP server. 
     */
    public String getLabel();
    
    /**
     * Returns the service name that will be applied to the connection.
     * This is something like <code>example.com</code> for users using
     * a jabber.com XMPP server.  
     */
    public String getServiceName();
    
    /** The resource identifier for this connection.  This is something like "Home". */
    public String getResource();
    
    /** A listener for Roster notifications from this configuration. */
    public EventListener<RosterEvent> getRosterListener();
}
