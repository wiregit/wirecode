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
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.client.impl.XMPPAddressRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AddressHandler implements EventListener<AddressEvent>, FeatureTransport.Handler<Address> {
    private static final Log LOG = LogFactory.getLog(AddressHandler.class);
    private final XMPPAddressRegistry addressRegistry;
    private final Map<String, Address> pendingAddresses;
    private Address address;
    private final Set<FriendConnection> connections;

    @Inject
    public AddressHandler(XMPPAddressRegistry addressRegistry,
                          FeatureRegistry featureRegistry) {
        this.addressRegistry = addressRegistry;
        this.pendingAddresses = new HashMap<String, Address>();
        connections = new HashSet<FriendConnection>();
        new AddressIQFeatureInitializer().register(featureRegistry);
    }
                                                      
    @Inject
    void register(ListenerSupport<FriendConnectionEvent> connectionEventListenerSupport,
                  ListenerSupport<AddressEvent> addressEventListenerSupport) {
        connectionEventListenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                switch (event.getType()) {
                    case CONNECTED:
                        connections.add(event.getSource()); 
                        break;
                    case DISCONNECTED:
                        connections.remove(event.getSource());
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
                        addressRegistry.put(new XMPPAddress(presence.getPresenceId()), address);
                        presence.addFeature(new AddressFeature(new XMPPAddress(presence.getPresenceId())));
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
            // TODO async?
            LOG.debugf("new address to publish: {0}", event);
            synchronized (this) {
                for(FriendConnection connection : connections) {
                    address = event.getData();
                    for(Friend friend : connection.getFriends()) {
                        for(FriendPresence presence : friend.getPresences().values()) {
                            if(presence.hasFeatures(LimewireFeature.ID)) {
                                try {
                                    FeatureTransport<Address> transport = presence.getTransport(AddressFeature.class);
                                    transport.sendFeature(presence, address);
                                } catch (FriendException e) {
                                    LOG.debugf("couldn't send address", e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private class AddressIQFeatureInitializer implements FeatureInitializer {
        @Override
        public void register(FeatureRegistry registry) {
            registry.add(AddressFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            synchronized (AddressHandler.this) {
                if (address != null) {
                    try {
                        FeatureTransport<Address> transport = friendPresence.getTransport(AddressFeature.class);
                        transport.sendFeature(friendPresence, address);
                    } catch (FriendException e) {
                        LOG.debugf(e, "couldn't send address to {0}" + friendPresence.getPresenceId());
                    }
                }
                if (pendingAddresses.containsKey(friendPresence.getPresenceId())) {
                    LOG.debugf("updating address on presence {0} to {1}", friendPresence.getPresenceId(), address);
                    Address pendingAddress = pendingAddresses.remove(friendPresence.getPresenceId());
                    addressRegistry.put(new XMPPAddress(friendPresence.getPresenceId()), pendingAddress);
                    friendPresence.addFeature(new AddressFeature(new XMPPAddress(friendPresence.getPresenceId()))); 
                }
            }
        }

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            addressRegistry.remove(new XMPPAddress(friendPresence.getPresenceId()));
            friendPresence.removeFeature(AddressFeature.ID);
        }
    }
}
