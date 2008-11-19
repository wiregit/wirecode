package org.limewire.xmpp.client.impl.messages.address;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.io.Address;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;

public class AddressIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(AddressIQListener.class);

    private final XMPPConnection connection;
    private volatile Address address;
    private final AddressFactory factory; 
    private final RosterEventHandler rosterEventHandler;
    private Map<String, Address> pendingAddresses;

    public AddressIQListener(XMPPConnection connection,
                             AddressFactory factory,
                             Address address) {
        this.connection = connection;
        this.factory = factory;
        this.address = address;
        this.rosterEventHandler = new RosterEventHandler();
        this.pendingAddresses = new HashMap<String, Address>();
    }

    public void processPacket(Packet packet) {
        AddressIQ iq = (AddressIQ)packet;
        if(iq.getType().equals(IQ.Type.SET)) {
            handleSet(iq);
        }
    }

    private void handleSet(AddressIQ iq) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling set");
        }
        handleAddressUpdate(iq);
    }

    private void handleAddressUpdate(AddressIQ iq) {
        synchronized (this) {
            User user = connection.getUser(StringUtils.parseBareAddress(iq.getFrom()));
            if (user != null) {
                Presence presence = user.getPresences().get(iq.getFrom());
                if(presence != null) {
                    LOG.debugf("updating address on presence {0} to {1}", presence.getJID(), iq.getAddress());
                    presence.addFeature(new AddressFeature(iq.getAddress()));
                } else {
                    LOG.debugf("address {0} for presence {1} is pending", iq.getAddress(), iq.getFrom());
                    pendingAddresses.put(iq.getFrom(), address);
                }
            }
        }
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof AddressIQ;
            }
        };
    }
    
    public void handleEvent(AddressEvent event) {
        if (event.getType().equals(Address.EventType.ADDRESS_CHANGED)) {
            // TODO async?
            LOG.debugf("new address to publish: {0}", event);
            synchronized (AddressIQListener.this) {
                address = event.getSource();
                for(User user : connection.getUsers()) {
                    for(Map.Entry<String, Presence> presenceEntry : user.getPresences().entrySet()) {
                        if(presenceEntry.getValue().hasFeatures(LimewireFeature.ID)) {
                            sendAddress(address, presenceEntry.getKey());
                        }
                    }
                }
            }
        }
    }
    
    private void sendAddress(Address address, String jid) {
        LOG.debugf("sending new address to {0}", jid);
        AddressIQ queryResult = new AddressIQ(address, factory);
        queryResult.setTo(jid);
        queryResult.setFrom(connection.getLocalJid());
        queryResult.setType(IQ.Type.SET);
        connection.sendPacket(queryResult);
    }
    
    public EventListener<RosterEvent> getRosterListener() {
        return rosterEventHandler;
    }

    private void userAdded(User user) {
        user.addPresenceListener(new EventListener<PresenceEvent>() {
            public void handleEvent(PresenceEvent event) {
                final Presence presence = event.getSource();
                if(event.getType().equals(Presence.EventType.PRESENCE_NEW) && 
                        presence.getType().equals(Presence.Type.available)) {
                    presence.getFeatureListenerSupport().addListener(new EventListener<FeatureEvent>() {
                        
                        @BlockingEvent
                        public void handleEvent(FeatureEvent event) {
                            if(event.getType() == Feature.EventType.FEATURE_ADDED) {
                                if(event.getSource().getID().equals(LimewireFeature.ID)) {
                                    synchronized (AddressIQListener.this) {
                                        if(address != null) {
                                            sendAddress(address, presence.getJID());
                                        }
                                        if(pendingAddresses.containsKey(presence.getJID())) {
                                            LOG.debugf("updating address on presence {0} to {1}", presence.getJID(), address);
                                            presence.addFeature(new AddressFeature(pendingAddresses.remove(presence.getJID())));    
                                        }
                                    }
                                }
                            }
                        }
                    });
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    synchronized (AddressIQListener.this) {
                        pendingAddresses.remove(presence.getJID());    
                    }
                }
            }
        });
    }
    
    private class RosterEventHandler implements EventListener<RosterEvent> {
        public void handleEvent(RosterEvent event) {
            if(event.getType().equals(User.EventType.USER_ADDED)) {
                userAdded(event.getSource());
            } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
                userDeleted(event.getSource().getId());
            } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
                userUpdated(event.getSource());
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void userUpdated(User user) {}

    @SuppressWarnings({"UnusedDeclaration"})
    private void userDeleted(String id) {}
}