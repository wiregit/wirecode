package org.limewire.xmpp.api.client;

public interface XMPPConnection {
    
    public enum ConnectionEvent {LOGIN}

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

    /**
     * Sets a new <code>&lt;presence&gt;</code> mode (i.e., status)
     * @param mode the new mode to set
     */
    public void setMode(Presence.Mode mode);

    /**
     * Add a user to the friend list
     * @param id cannot be null
     * @param name can be null
     * @throws XMPPException
     */
    public void addUser(String id, String name) throws XMPPException;
    
    /**
     * Remove a user from the friend list
     * @param id cannot be null
     * @throws XMPPException
     */
    public void removeUser(String id) throws XMPPException;
}
