package org.limewire.xmpp.api.client;

import org.limewire.listener.EventListener;


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
    public void addRosterListener(EventListener<RosterEvent> rosterListener);

    /**
     * Sets a new <code>&lt;presence&gt;</code> mode (i.e., status)
     * @param mode the new mode to set
     */
    public void setMode(Presence.Mode mode);
}
