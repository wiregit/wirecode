package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
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

    @Inject
    public XMPPAddressResolver(XMPPService xmppService) {
        this.xmppService = xmppService;
    }
    
    @Inject
    public void register(SocketsManager socketsManager) {
        socketsManager.registerResolver(this);
    }
    
    @Override
    public boolean canResolve(Address address) {
        if (address instanceof XMPPAddress) {
            XMPPAddress friendIdAddress = (XMPPAddress)address;
            return !getResolvedAddresses(friendIdAddress).isEmpty();
        }
        return false;
    }
    
    private List<Address> getResolvedAddresses(XMPPAddress address) {
        String id = address.getId();
        List<Address> addresses = new ArrayList<Address>(1);
        for (XMPPConnection connection : xmppService.getConnections()) {
            User user = connection.getUser(id);
            if (user != null) {
                for (Entry<String, Presence> entry : user.getPresences().entrySet()) {
                    Address resolvedAddress = getMatchingAddress(address, entry.getKey(), entry.getValue());
                    if (resolvedAddress != null) {
                        addresses.add(resolvedAddress);
                    }
                }
            }
        }
        return addresses;
    }

    /**
     * Returns address of presence if presence is {@link FriendPresence} and the resource
     * id matches the one in <code>address</code> and the address is available in
     * the presence.
     */
    private Address getMatchingAddress(XMPPAddress address, String resourceId, Presence presence) {
        if (!(presence instanceof FriendPresence)) {
            return  null;
        }
        String originalId = address.getId();
        int slash = originalId.indexOf('/');
        if (slash == -1) {
            LOG.debugf("no slash in full id: {0}", originalId);
            return null;
        }
        int toOffset = Math.min(originalId.length(), slash + 10);
        if (originalId.startsWith(resourceId, toOffset)) {
            return ((FriendPresence)presence).getPresenceAddress();
        }
        return null;
    }

    @Override
    public void resolve(Address address, int timeout, AddressResolutionObserver observer) {
        XMPPAddress xmppAddress = (XMPPAddress)address;
        List<Address> resolvedAddresses = getResolvedAddresses(xmppAddress);
        if (resolvedAddresses.isEmpty()) {
            observer.handleIOException(new IOException("Could not be resolved"));
        } else {
            observer.resolved(resolvedAddresses.toArray(new Address[resolvedAddresses.size()]));
        }
    }

}
