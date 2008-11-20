package org.limewire.xmpp.api.client;

/**
 * Describes an interface for managing XMPP connections. Only one connection
 * can be logged in at a time.
 */
public interface XMPPService {

    /**
     * Attempts to log in a connection using the specified configuration.
     * Any existing connections will be logged out first.
     */
    public void login(XMPPConnectionConfiguration configuration);

    /**
     * Logs out any existing connections.
     */
    public void logout();

    /**
     * Returns the logged in connection, or null if there isn't one.
     */
    public XMPPConnection getLoggedInConnection();

    /**
     * Sets the error listener that will be informed if an error occurs
     * while logging in a connection.
     */
    public void setXmppErrorListener(XMPPErrorListener errorListener);
}
