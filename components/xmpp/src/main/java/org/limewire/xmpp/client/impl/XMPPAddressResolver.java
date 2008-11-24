package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Map.Entry;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.FeatureEvent.Type;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.io.Address;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.net.address.AddressResolver;
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

    private final EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster;
    
    private final ConnectivyFeatureListener connectivyFeatureListener = new ConnectivyFeatureListener();

    @Inject
    public XMPPAddressResolver(XMPPService xmppService, EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster) {
        this.xmppService = xmppService;
        this.connectivityEventBroadcaster = connectivityEventBroadcaster;
        
    }
    
    @Inject void register(SocketsManager socketsManager, ListenerSupport<FeatureEvent> featureSupport) {
        socketsManager.registerResolver(this);
        featureSupport.addListener(connectivyFeatureListener);
    }
    
    @Override
    public boolean canResolve(Address address) {
        if (address instanceof XMPPAddress) {
            XMPPAddress friendIdAddress = (XMPPAddress)address;
            boolean canResolve = getPresence(friendIdAddress) != null;
            LOG.debugf("could/could not resolve {0}: {1}", address, canResolve);
            return canResolve;
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
        String id = address.getId();
        XMPPConnection connection = xmppService.getActiveConnection();
        if(connection == null)
            return null;
        User user = connection.getUser(id);
        if(user != null) {
            for(Entry<String, FriendPresence> entry : user.getFriendPresences().entrySet()) {
                FriendPresence resolvedPresence =
                    getMatchingPresence(address, entry.getKey(), entry.getValue());
                if(resolvedPresence != null) {
                    return resolvedPresence;
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
    private FriendPresence getMatchingPresence(XMPPAddress address, String resourceId, FriendPresence presence) {
        String originalId = address.getFullId();
        int slash = originalId.indexOf('/');
        if (slash == -1) {
            LOG.debugf("no slash in full id: {0}", originalId);
            return null;
        }
        // only look at the first 5 characters of the resource string, since jabber servers
        // add their own random characters
        int toOffset = Math.min(originalId.length(), slash + 5);
        if (originalId.substring(0, toOffset).equals(resourceId.substring(0, toOffset))) {
            // only return address if auth-token is available too, otherwise
            // the address is worthless still
            Feature addressFeature = presence.getFeature(AddressFeature.ID);
            Feature authTokenFeature = presence.getFeature(AuthTokenFeature.ID);
            if(addressFeature != null && authTokenFeature != null) {
                return presence;
            }
        }
        return null;
    }

    @Override
    public void resolve(Address address, AddressResolutionObserver observer) {
        XMPPAddress xmppAddress = (XMPPAddress)address;
        FriendPresence resolvedPresence = getPresence(xmppAddress);
        if (resolvedPresence == null) {
            observer.handleIOException(new IOException("Could not be resolved"));
        } else {
            observer.resolved(((AddressFeature)resolvedPresence.getFeature(AddressFeature.ID)).getFeature());
        }
    }

    private class ConnectivyFeatureListener implements EventListener<FeatureEvent> {
        @Override
        public void handleEvent(FeatureEvent event) {
            if (event.getType() != Type.ADDED) {
                return;
            }
            URI id = event.getData().getID();
            if (id.equals(AuthTokenFeature.ID) || id.equals(AddressFeature.ID)) {
                FriendPresence presence = event.getSource();
                if (presence.hasFeatures(AuthTokenFeature.ID, AddressFeature.ID)) {
                    LOG.debugf("presence with address and auth-token became available: {0}", presence.getPresenceId());
                    connectivityEventBroadcaster.broadcast(new ConnectivityChangeEvent());    
                }
            }
        }
    }
}