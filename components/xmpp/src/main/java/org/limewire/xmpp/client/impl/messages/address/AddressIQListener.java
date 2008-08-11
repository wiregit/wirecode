package org.limewire.xmpp.client.impl.messages.address;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.Address;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterListener;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.LimePresenceImpl;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AddressIQListener implements PacketListener, EventListener<AddressEvent>, RosterListener {
    private static final Log LOG = LogFactory.getLog(AddressIQListener.class);

    private final XMPPConnection connection;
    private volatile Address address;
    private final AddressFactory factory;
    
    private final Map<String, AddressIQ> getRequests = new HashMap<String, AddressIQ>(); 
    // TODO get rid of subscribedJids, and just use limePresences
    private final Set<String> subscribedJids = new HashSet<String>();
    private final Map<String, LimePresenceImpl> limePresences = new HashMap<String, LimePresenceImpl>();

    public AddressIQListener(XMPPConnection connection,
                             AddressFactory factory,
                             Address address) {
        this.connection = connection;
        this.factory = factory;
        this.address = address;
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
                if(LOG.isDebugEnabled()) {
                    LOG.debug("updating address on presence " + presence.getJID() + " to " + address);
                }
                presence.setAddress(iq.getAddress());
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
    
    public void handleEvent(AddressEvent event) {
        if(event.getType().equals(Address.EventType.ADDRESS_CHANGED)) {
            // TODO async?
            LOG.debugf("new address to publish: {0}", event.getSource().toString());
            synchronized (this) {
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
    
    private void sendAddress(Address address, String jid) {
        LOG.debugf("sending new address to {0}", jid);
        AddressIQ queryResult = new AddressIQ(address, factory);
        queryResult.setTo(jid);
        queryResult.setFrom(connection.getUser());
        queryResult.setType(IQ.Type.SET);
        connection.sendPacket(queryResult);
    }

    public void register(XMPPService xmppService) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void userAdded(User user) {
        user.addPresenceListener(new PresenceListener() {
            public void presenceChanged(Presence presence) {
                if(presence.getType().equals(Presence.Type.available)) {
                    if(presence instanceof LimePresence) {
                        synchronized (this) {
                            limePresences.put(presence.getJID(), (LimePresenceImpl)presence);
                        }
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    synchronized (this) {
                        subscribedJids.remove(presence.getJID());
                        limePresences.remove(presence.getJID());
                    }
                }
            }
        });
    }

    public void userUpdated(User user) {}

    public void userDeleted(String id) {}
}
