package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.Network;
import org.limewire.listener.EventListener;

/**
 * Allows the xmpp service user to provide configuration for xmpp login.
 */
public interface XMPPConnectionConfiguration extends Network {
    public boolean isDebugEnabled();
    public String getUsername();
    public String getPassword();
    public String getHost();
    public int getPort();
    public String getServiceName();
    public String getResource();
    public EventListener<RosterEvent> getRosterListener();
}
