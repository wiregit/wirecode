package org.limewire.xmpp.client;

public interface XMPPConnection {
    public XMPPConnectionConfiguration getConfiguration();
    public void login() throws XMPPException;
    public void logout();

    boolean isLoggedIn();
}
