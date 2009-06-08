package org.limewire.xmpp.api.client;

import java.util.Collection;

import org.limewire.concurrent.ListeningFuture;

public interface XMPPConnection {
    
    public XMPPConnectionConfiguration getConfiguration();

    /**
     * Logs a user into the XMPP server.
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException if an error occurs
     */
    public ListeningFuture<Void> login();
    
    /**
     * Logs a user out of the XMPP server.
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException if an error occurs
     */
    public ListeningFuture<Void> logout();

    /**
     * @return true if this connection is logged in.
     */
    public boolean isLoggedIn();
    
    /**
     * @return true if this connection is now logging in.
     */
    public boolean isLoggingIn();

    /**
     * Sets a new <code>&lt;presence&gt;</code> mode (i.e., status).
     * @param mode the new mode to set
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException
     * if there is an error sending the XMPP message.
     */
    public ListeningFuture<Void> setMode(XMPPPresence.Mode mode);

    /**
     * Add a friend to the friend list.
     * @param id cannot be null
     * @param name can be null
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException
     * if there is an error sending the XMPP message.
     */
    public ListeningFuture<Void> addFriend(String id, String name);
    
    /**
     * Remove a friend from the friend list.
     * @param id cannot be null
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException
     * if there is an error sending the XMPP message.
     */
    public ListeningFuture<Void> removeFriend(String id);

    /**
     * Returns the friend belonging to <code>id</code>. <code>id</code>
     * is the friend's email address.
     * 
     * @return null if id is not registered on this connection.
     */
    public XMPPFriend getFriend(String id);

    /**
     * @return a copy of the current Collection of friends. Does NOT stay up to
     * date with changes.
     */
    public Collection<XMPPFriend> getFriends();
}