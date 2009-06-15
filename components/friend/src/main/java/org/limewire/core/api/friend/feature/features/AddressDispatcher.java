package org.limewire.core.api.friend.feature.features;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.core.api.friend.address.FriendAddress;
import org.limewire.core.api.friend.address.FriendAddressRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
/**
 * The <code>AddressDispatcher</code> does dispatching of local and remote <code>Address</code>s.<BR>
 *
 * It dispatches the local <code>Address</code> out to all <code>FriendPresence</code>s that support <code>AddressFeature</code>.<BR>
 * It dispatches remote <code>Addresses</code> received from friends to the <code>FriendAddressRegistry</code>,
 * which maps <code>FriendAddress</code>s (conceptually a friend id + GUID) to a <code>Connectable</code> or
 * <code>FirewalledAddress</code>.
 */
public class AddressDispatcher implements EventListener<AddressEvent>, FeatureTransport.Handler<Address> {
    private static final Log LOG = LogFactory.getLog(AddressDispatcher.class);
    private final FriendAddressRegistry addressRegistry;
    private final Map<String, Address> pendingAddresses;
    private Address localAddress;
    private final Set<FriendConnection> connections;

    @Inject
    public AddressDispatcher(FriendAddressRegistry addressRegistry,
                          FeatureRegistry featureRegistry) {
        this.addressRegistry = addressRegistry;
        this.pendingAddresses = new HashMap<String, Address>();
        this.connections = new HashSet<FriendConnection>();
        new AddressFeatureInitializer().register(featureRegistry);
    }

    @Inject
    void register(ListenerSupport<FriendConnectionEvent> connectionEventListenerSupport,
                  ListenerSupport<AddressEvent> addressEventListenerSupport) {
        connectionEventListenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                synchronized (AddressDispatcher.this) {
                    switch (event.getType()) {
                    case CONNECTED:
                        connections.add(event.getSource()); 
                        break;
                    case DISCONNECTED:
                        connections.remove(event.getSource());
                    }
                }
            }
        });
        addressEventListenerSupport.addListener(this);
    }

    public void featureReceived(String from, Address address) {
        synchronized (this) {
            for(FriendConnection connection : connections) {
                Friend friend = connection.getFriend(StringUtils.parseBareAddress(from));
                if (friend != null) {
                    FriendPresence presence = friend.getPresences().get(from);
                    if(presence != null) {
                        LOG.debugf("updating address on presence {0} to {1}", presence.getPresenceId(), address);
                        addressRegistry.put(new FriendAddress(presence.getPresenceId()), address);
                        presence.addFeature(new AddressFeature(new FriendAddress(presence.getPresenceId())));
                    } else {
                        LOG.debugf("address {0} for presence {1} is pending", address, from);
                        pendingAddresses.put(from, address);
                    }
                } else {
                    LOG.debugf("no friend for: {0}", from);
                }
            }
        }
    }

    @Override
    public void handleEvent(AddressEvent event) {
        if (event.getType().equals(AddressEvent.Type.ADDRESS_CHANGED)) {
            LOG.debugf("new localAddress to publish: {0}", event);
            synchronized (this) {
                localAddress = event.getData();
                for(FriendConnection connection : connections) {
                    for(Friend friend : connection.getFriends()) {
                        for(FriendPresence presence : friend.getPresences().values()) {
                            if(presence.hasFeatures(LimewireFeature.ID)) {
                                try {
                                    FeatureTransport<Address> transport = presence.getTransport(AddressFeature.class);
                                    transport.sendFeature(presence, localAddress);
                                } catch (FriendException e) {
                                    LOG.debugf("couldn't send localAddress", e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private class AddressFeatureInitializer implements FeatureInitializer {
        @Override
        public void register(FeatureRegistry registry) {
            registry.add(AddressFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            synchronized (AddressDispatcher.this) {
                if (localAddress != null) {
                    try {
                        FeatureTransport<Address> transport = friendPresence.getTransport(AddressFeature.class);
                        transport.sendFeature(friendPresence, localAddress);
                    } catch (FriendException e) {
                        LOG.debugf(e, "couldn't send localAddress to {0}" + friendPresence.getPresenceId());
                    }
                }
                if (pendingAddresses.containsKey(friendPresence.getPresenceId())) {
                    LOG.debugf("updating address on presence {0} to {1}", friendPresence.getPresenceId(), localAddress);
                    Address pendingAddress = pendingAddresses.remove(friendPresence.getPresenceId());
                    addressRegistry.put(new FriendAddress(friendPresence.getPresenceId()), pendingAddress);
                    friendPresence.addFeature(new AddressFeature(new FriendAddress(friendPresence.getPresenceId()))); 
                }
            }
        }

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            addressRegistry.remove(new FriendAddress(friendPresence.getPresenceId()));
            friendPresence.removeFeature(AddressFeature.ID);
        }
    }
}
