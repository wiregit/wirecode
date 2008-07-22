package org.limewire.xmpp.client.impl.messages.address;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.listener.EventListener;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NetworkManagerEvent;
import org.limewire.net.address.Address;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.xmlpull.v1.XmlPullParserException;

public class AddressIQListener implements PacketListener, EventListener<NetworkManagerEvent> {
    private static final Log LOG = LogFactory.getLog(AddressIQListener.class);

    private XMPPConnection connection;
    private final List<AddressIQ> getRequests = new ArrayList<AddressIQ>();
    private volatile Address address;
    private final AddressFactory factory;

    public AddressIQListener(XMPPConnection connection,
                             /*NetworkManager networkManager,*/ AddressFactory factory) {
        this.connection = connection;
        this.factory = factory;
        //networkManager.addListener(this);
    }

    public void processPacket(Packet packet) {
        AddressIQ iq = (AddressIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                handleGet(iq);
            } else if(iq.getType().equals(IQ.Type.RESULT)) {
                handleResult(iq);
            } else if(iq.getType().equals(IQ.Type.SET)) {
                // TODO
                //handleSet(iq);
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

    private void handleResult(AddressIQ addressIQ) {
           
    }

    private void handleGet(AddressIQ packet) throws IOException, XmlPullParserException {
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

    public void handleEvent(NetworkManagerEvent event) {
        if(event instanceof AddressEvent) {
            if(event.getType().equals(NetworkManager.EventType.ADDRESS_CHANGE)) {
                // TODO async?
                synchronized (this) {
                    address = ((AddressEvent)event).getAddress();
                    for(AddressIQ getRequest : getRequests) {
                        sendResult(getRequest);        
                    }
                    getRequests.clear();
                }
                // TODO notify all buddies?                
            }
        }
    }
}
