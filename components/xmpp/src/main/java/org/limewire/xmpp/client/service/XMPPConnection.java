package org.limewire.xmpp.client.service;

import org.limewire.xmpp.client.impl.XMPPException;

public interface XMPPConnection {
    public XMPPConnectionConfiguration getConfiguration();
    public void login() throws XMPPException;
    public void logout();
    public boolean isLoggedIn();
    public void addRosterListener(RosterListener rosterListener);
}
