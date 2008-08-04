package org.limewire.xmpp.client.service;

import org.limewire.xmpp.client.impl.XMPPException;

public interface XMPPConnection {
    public XMPPConnectionConfiguration getConfiguration();

    /**
     * logs a user into the xmpp server; blocking call.
     * @throws XMPPException
     */
    public void login() throws XMPPException;
    
    /**
     * logs a user out of the xmpp server; blocking call.
     * @throws XMPPException
     */
    public void logout();
    public boolean isLoggedIn();
    public void addRosterListener(RosterListener rosterListener);
}
