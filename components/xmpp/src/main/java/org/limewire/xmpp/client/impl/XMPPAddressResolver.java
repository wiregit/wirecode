package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.util.Map.Entry;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
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
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.PresenceEvent;

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
    
    private final PresenceHandler presenceHandler = new PresenceHandler();

    @Inject
    public XMPPAddressResolver(XMPPService xmppService, EventBroadcaster<ConnectivityChangeEvent> connectivityEventBroadcaster) {
        this.xmppService = xmppService;
        this.connectivityEventBroadcaster = connectivityEventBroadcaster;
        
    }
    
    @Inject
    public void register(SocketsManager socketsManager,  ListenerSupport<RosterEvent> rosterListenerSupport) {
        socketsManager.registerResolver(this);
        rosterListenerSupport.addListener(new RosterHandler());
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

    private class RosterHandler implements EventListener<RosterEvent> {

        @Override
        public void handleEvent(RosterEvent event) {
            if (event.getType() == User.EventType.USER_ADDED) {
                User user = event.getSource();
                LOG.debugf("user added: {0}", user);
                user.addPresenceListener(presenceHandler);
            }
        }
        
    }
    
    private class PresenceHandler implements EventListener<PresenceEvent> {

        @Override
        public void handleEvent(PresenceEvent event) {
            final Presence presence = event.getSource();
            if (presence.getMode() == Presence.Mode.available && event.getType() == Presence.EventType.PRESENCE_NEW) {
                presence.getFeatureListenerSupport().addListener(new EventListener<FeatureEvent>() {
                    @Override
                    public void handleEvent(FeatureEvent event) {
                        if (presence.hasFeatures(AuthTokenFeature.ID, AddressFeature.ID)) {
                            LOG.debugf("presence with address and auth-token became available: {0}", presence.getJID());
                            connectivityEventBroadcaster.broadcast(new ConnectivityChangeEvent());    
                        }
                    }
                });                
            }
        }
    }
}