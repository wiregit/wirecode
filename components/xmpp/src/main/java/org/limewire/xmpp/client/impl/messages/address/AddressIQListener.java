package org.limewire.xmpp.client.impl.messages.address;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.listener.BlockingEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.client.impl.LimePresenceImpl;
import org.xmlpull.v1.XmlPullParserException;

public class AddressIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(AddressIQListener.class);

    private final XMPPConnection connection;
    private volatile Address address;
    private final AddressFactory factory;
    
    private final Map<String, AddressIQ> getRequests = new HashMap<String, AddressIQ>(); 
    // TODO get rid of subscribedJids, and just use limePresences
    private final Set<String> subscribedJids = new HashSet<String>();
    private final Map<String, LimePresenceImpl> limePresences = new HashMap<String, LimePresenceImpl>();
    private final RosterEventHandler rosterEventHandler;

    public AddressIQListener(XMPPConnection connection,
                             AddressFactory factory,
                             Address address) {
        this.connection = connection;
        this.factory = factory;
        this.address = address;
        this.rosterEventHandler = new RosterEventHandler();
    }

    public void processPacket(Packet packet) {
        AddressIQ iq = (AddressIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                handleGet(iq);
            } else if(iq.getType().equals(IQ.Type.RESULT)) {
                handleResult(iq);
            } else if(iq.getType().equals(IQ.Type.SET)) {
                handleSet(iq);
            } else if(iq.getType().equals(IQ.Type.ERROR)) {
                // TODO
                //handleError(iq);
            } else {
                // TODO
                //sendError(packet);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            // TODO
            //sendError(packet);
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);
            // TODO
            //sendError(packet);
        }
    }

    private void handleSet(AddressIQ iq) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling set");
        }
        handleAddressUpdate(iq);
    }
    
    private void handleResult(AddressIQ addressIQ) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling result");
        }
        handleAddressUpdate(addressIQ);
    }

    private void handleAddressUpdate(AddressIQ iq) {
        synchronized (this) {
            LimePresenceImpl presence = limePresences.get(iq.getFrom());
            if(presence != null) {
                if(iq.getAddress() != null) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("updating address on presence " + presence.getJID() + " to " + iq.getAddress());
                    }
                    presence.setPresenceAddress(iq.getAddress());
                }
            }
        }
    }
    
    private void handleGet(AddressIQ packet) throws IOException, XmlPullParserException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling address subscription from " + packet.getFrom());
        }
        synchronized (this) {
            if(subscribedJids.add(packet.getFrom())) {
                if(address == null) {
                    // we do not yet know our external address
                    getRequests.put(packet.getFrom(), packet);
                } else {
                    sendResult(packet);
                }
            }
        }
    }

    private void sendResult(AddressIQ packet) {
        AddressIQ queryResult = new AddressIQ(address, factory);
        queryResult.setTo(packet.getFrom());
        queryResult.setFrom(packet.getTo());
        queryResult.setPacketID(packet.getPacketID());
        queryResult.setType(IQ.Type.RESULT);
        connection.sendPacket(queryResult);
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof AddressIQ;
            }
        };
    }
    
    public EventListener<AddressEvent> getAddressListener() {
        return new EventListener<AddressEvent>() {
            public void handleEvent(AddressEvent event) {
                if(event.getType().equals(Address.EventType.ADDRESS_CHANGED)) {
                    // TODO async?
                    LOG.debugf("new address to publish: {0}", event.getSource().toString());
                    synchronized (AddressIQListener.this) {
                        address = event.getSource();
                        for(String jid : subscribedJids) {
                            if(getRequests.containsKey(jid)) {
                                sendResult(getRequests.remove(jid));
                            } else {
                                sendAddress(address, jid);
                            }
                        }
                    }
                }
            }
        };
    }
    
    private void sendAddress(Address address, String jid) {
        LOG.debugf("sending new address to {0}", jid);
        AddressIQ queryResult = new AddressIQ(address, factory);
        queryResult.setTo(jid);
        queryResult.setFrom(connection.getUser());
        queryResult.setType(IQ.Type.SET);
        connection.sendPacket(queryResult);
    }
    
    public EventListener<RosterEvent> getRosterListener() {
        return rosterEventHandler;
    }

    private void userAdded(User user) {
        user.addPresenceListener(new EventListener<PresenceEvent>() {

            @BlockingEvent
            public void handleEvent(PresenceEvent event) {
                Presence presence = event.getSource();
                if(presence.getType().equals(Presence.Type.available)) {
                    if(presence instanceof LimePresence) {
                        synchronized (AddressIQListener.this) {
                            limePresences.put(presence.getJID(), (LimePresenceImpl)presence);
                        }
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    synchronized (AddressIQListener.this) {
                        subscribedJids.remove(presence.getJID());
                        limePresences.remove(presence.getJID());
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
