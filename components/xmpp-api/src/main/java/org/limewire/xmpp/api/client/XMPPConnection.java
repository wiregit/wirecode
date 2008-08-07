package org.limewire.xmpp.api.client;


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
