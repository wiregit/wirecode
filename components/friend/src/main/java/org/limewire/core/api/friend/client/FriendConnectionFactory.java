package org.limewire.core.api.friend.client;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.friend.client.FriendConnection;


/**
 * Describes an interface for managing XMPP connections. Only one connection
 * can be logged in at a time.
 */
public interface FriendConnectionFactory {

    /**
     * Attempts to log in a connection using the specified configuration.
     * Any existing connections will be logged out first.
     * 
     * @param configuration the XMPPConnectionConfiguration to use; can not be null
     *
     * @return a {@link ListeningFuture} of {@link FriendConnection}
     * 
     * The ExecutionException will be to an XMPPException if an error occurs
     */
    public ListeningFuture<FriendConnection> login(FriendConnectionConfiguration configuration);

    public void register(FriendConnectionFactoryRegistry registry);
}
