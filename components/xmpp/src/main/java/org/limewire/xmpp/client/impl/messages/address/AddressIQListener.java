package org.limewire.xmpp.client.impl.messages.address;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.io.Address;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.client.impl.XMPPConnectionImpl;

public class AddressIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(AddressIQListener.class);

    private final XMPPConnectionImpl connection;
    private volatile Address address;
    private final AddressFactory factory; 
    private final Map<String, Address> pendingAddresses;
    private final ListenerSupport<FeatureEvent> featureSupport;
    private final EventListener<FeatureEvent> featureListener;

    public AddressIQListener(XMPPConnectionImpl connection,
                             AddressFactory factory,
                             Address address,
                             ListenerSupport<FeatureEvent> featureSupport) {
        this.connection = connection;
        this.factory = factory;
        this.address = address;
        this.pendingAddresses = new HashMap<String, Address>();
        this.featureSupport = featureSupport;
        this.featureListener = new FeatureListener();
        featureSupport.addListener(featureListener);
    }
    
    public void dispose() {
        featureSupport.removeListener(featureListener);
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
                FriendPresence presence = user.getFriendPresences().get(iq.getFrom());
                if(presence != null) {
                    LOG.debugf("updating address on presence {0} to {1}", presence.getPresenceId(), iq.getAddress());
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
                    for(Map.Entry<String, FriendPresence> presenceEntry : user.getFriendPresences().entrySet()) {
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

    private class FeatureListener implements EventListener<FeatureEvent> {

        @BlockingEvent
        public void handleEvent(FeatureEvent featureEvent) {
            FriendPresence presence = featureEvent.getSource();
            String jid = presence.getPresenceId();

            switch(featureEvent.getType()) {
                case ADDED:
                    if (featureEvent.getData().getID().equals(LimewireFeature.ID)) {
                        synchronized (AddressIQListener.this) {
                            if (address != null) {
                                sendAddress(address, jid);
                            }
                            if (pendingAddresses.containsKey(jid)) {
                                LOG.debugf("updating address on presence {0} to {1}", jid, address);
                                presence.addFeature(new AddressFeature(pendingAddresses.remove(jid)));
                            }
                        }
                    }
                    break;
                case REMOVED:
                    synchronized (AddressIQListener.this) {
                        pendingAddresses.remove(jid);
                    }
                    break;
            }
        }
    }
}