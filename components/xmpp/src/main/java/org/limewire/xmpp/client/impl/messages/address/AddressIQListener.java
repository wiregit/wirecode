package org.limewire.xmpp.client.impl.messages.address;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.listener.EventListener;
import org.limewire.net.address.Address;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.client.impl.LimePresenceImpl;
import org.limewire.xmpp.client.impl.UserImpl;
import org.limewire.xmpp.client.service.LimePresence;
import org.limewire.xmpp.client.service.Presence;
import org.limewire.xmpp.client.service.PresenceListener;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.User;
import org.limewire.xmpp.client.service.XMPPService;
import org.xmlpull.v1.XmlPullParserException;

public class AddressIQListener implements PacketListener, EventListener<AddressEvent>, RosterListener {
    private static final Log LOG = LogFactory.getLog(AddressIQListener.class);

    private XMPPConnection connection;
    private final List<AddressIQ> getRequests = new ArrayList<AddressIQ>();
    private volatile Address address;
    private final AddressFactory factory;
    private final HashMap<String, UserImpl> users;
    private final Set<String> subscribedJids = new HashSet<String>();
    private final Map<String, LimePresenceImpl> limePresences = new HashMap<String, LimePresenceImpl>();

    public AddressIQListener(XMPPConnection connection,
                             AddressFactory factory, HashMap<String, UserImpl> users) {
        this.connection = connection;
        this.factory = factory;
        this.users = users;
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
        synchronized (this) {
            LimePresenceImpl presence = limePresences.get(iq.getFrom());
            if(presence != null) {
                presence.setAddress(address);
            }
        }
    }

    private void handleResult(AddressIQ addressIQ) {
           
    }

    private void _handleGet(AddressIQ packet) throws IOException, XmlPullParserException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling address get " + packet.getPacketID());
        }
        synchronized (this) {
            if(address == null) {
                // we do not yet know our external address
                getRequests.add(packet);
            } else {
                sendResult(packet);
            }
        }
    }
    
    private void handleGet(AddressIQ packet) throws IOException, XmlPullParserException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling address subscription for " + packet.getFrom());
        }
        synchronized (this) {
            if(subscribedJids.add(packet.getFrom())) {
                if(address != null) {
                    sendAddress(address, packet.getFrom());
                } else {
                    // TODO send ack
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

    public void __handleEvent(AddressEvent event) {
        if(event.getType().equals(Address.EventType.ADDRESS_CHANGED)) {
            // TODO async?
            synchronized (this) {
                address = event.getSource();
                for(AddressIQ getRequest : getRequests) {
                    sendResult(getRequest);
                }
                getRequests.clear();
            }
            // TODO notify all buddies?
        }
    }
    
    public void _handleEvent(AddressEvent event) {
        if(event.getType().equals(Address.EventType.ADDRESS_CHANGED)) {
            // TODO async?
            synchronized (this) { // TODO synch on users?
                address = event.getSource();
                for(UserImpl user : users.values()) {
                    for(Presence presence : user.getPresences().values()) {
                        if(presence instanceof LimePresence) {
                            sendAddress(address, (LimePresence)presence);            
                        }
                    }
                }
                getRequests.clear();
            }
        }
    }
    
    public void handleEvent(AddressEvent event) {
        if(event.getType().equals(Address.EventType.ADDRESS_CHANGED)) {
            // TODO async?
            synchronized (this) {
                address = event.getSource();
                for(String jid : subscribedJids) {
                    sendAddress(address, jid);
                }
            }
        }
    }
    
    private void sendAddress(Address address, LimePresence presence) {
        AddressIQ queryResult = new AddressIQ(address, factory);
        queryResult.setTo(presence.getJID());
        queryResult.setFrom(connection.getUser());
        queryResult.setType(IQ.Type.SET);
        connection.sendPacket(queryResult);
    }
    
    private void sendAddress(Address address, String jid) {
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
                if(presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                    if(presence instanceof LimePresence) {
                        synchronized (this) {
                            limePresences.put(presence.getJID(), (LimePresenceImpl)presence);
                        }
                    }
                } else if(presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
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
