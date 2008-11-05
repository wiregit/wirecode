package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.util.Map.Entry;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.io.Address;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.net.address.AddressResolver;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Resolves addresses of type {@link XMPPAddress} by looking up the full jabber id 
 * including resource in the logged in users.
 */
@Singleton
public class XMPPAddressResolver implements AddressResolver {

    private final static Log LOG = LogFactory.getLog(XMPPAddressResolver.class);
    
    private final XMPPService xmppService;

    private EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster;

    @Inject
    public XMPPAddressResolver(XMPPService xmppService) {
        this.xmppService = xmppService;
    }
    
    @Inject
    public void register(SocketsManager socketsManager,  EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster) {
        this.connectivityEventBroadcaster = connectivityEventBroadcaster;
        socketsManager.registerResolver(this);
    }
    
    @Override
    public boolean canResolve(Address address) {
        if (address instanceof XMPPAddress) {
            XMPPAddress friendIdAddress = (XMPPAddress)address;
            return getPresence(friendIdAddress) != null;
        }
        return false;
    }
    
    /**
     * Returns the friend presence belonging to an <code>address</code>.
     * 
     * @return null if not presence is found for the address, i.e. the user
     * is not online for example
     */
    public FriendPresence getPresence(XMPPAddress address) {
        String id = address.getFullId();
        for (XMPPConnection connection : xmppService.getConnections()) {
            User user = connection.getUser(id);
            if (user != null) {
                for (Entry<String, Presence> entry : user.getPresences().entrySet()) {
                    FriendPresence resolvedPresence = getMatchingPresence(address, entry.getKey(), entry.getValue());
                    if (resolvedPresence != null) {
                        return resolvedPresence;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns presence if presence is {@link FriendPresence} and the resource
     * id matches the one in <code>address</code> and the address is available in
     * the presence.
     * 
     * Also ensures that auth-token and presence address are set.
     */
    private FriendPresence getMatchingPresence(XMPPAddress address, String resourceId, Presence presence) {
        if (!(presence instanceof FriendPresence)) {
            return  null;
        }
        String originalId = address.getId();
        int slash = originalId.indexOf('/');
        if (slash == -1) {
            LOG.debugf("no slash in full id: {0}", originalId);
            return null;
        }
        // only look at the first 10 characters of the resource string, since jabber servers
        // add their own random characters
        int toOffset = Math.min(originalId.length(), slash + 10);
        if (originalId.startsWith(resourceId, toOffset)) {
            FriendPresence friendPresence = (FriendPresence)presence;
            // only return address if auth-token is available too, otherwise
            // the address is worthless still
            if (friendPresence.getAuthToken() != null && friendPresence.getPresenceAddress() != null) {
                return friendPresence;
            }
        }
        return null;
    }

    @Override
    public void resolve(Address address, int timeout, AddressResolutionObserver observer) {
        XMPPAddress xmppAddress = (XMPPAddress)address;
        FriendPresence resolvedPresence = getPresence(xmppAddress);
        if (resolvedPresence == null) {
            observer.handleIOException(new IOException("Could not be resolved"));
        } else {
            observer.resolved(resolvedPresence.getPresenceAddress());
        }
    }

}
