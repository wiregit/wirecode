package org.limewire.xmpp.client.impl;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.net.address.Address;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.address.AddressIQ;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.LimePresence;
import org.xmlpull.v1.XmlPullParserException;

public class LimePresenceImpl extends PresenceImpl implements LimePresence {

    private static final Log LOG = LogFactory.getLog(LimePresenceImpl.class);
    
    private Address address;

    LimePresenceImpl(org.jivesoftware.smack.packet.Presence presence, XMPPConnection connection) {
        super(presence, connection);
    }
    
    public Address getAddress() {
        return address;
    }
    
    void sendGetAddress() {
        if(LOG.isInfoEnabled()) {
            LOG.info("getting address from " + getJID() + "...");
        }
        final AddressIQ addressIQ = new AddressIQ();
        addressIQ.setType(IQ.Type.GET);
        addressIQ.setTo(getJID());
        addressIQ.setPacketID(IQ.nextID());
        final PacketCollector collector = connection.createPacketCollector(
            new PacketIDFilter(addressIQ.getPacketID()));         
        connection.sendPacket(addressIQ);
        final AddressIQ response = (AddressIQ) collector.nextResult();
        address = response.getAddress();
        collector.cancel();
    }

    public void sendFile(FileMetaData file) {
        if(LOG.isInfoEnabled()) {
            LOG.info("sending file " + file.toString() + " to " + getJID());
        }
        final FileTransferIQ transferIQ = new FileTransferIQ(file, FileTransferIQ.TransferType.OFFER);
        transferIQ.setType(IQ.Type.GET);
        transferIQ.setTo(getJID());
        transferIQ.setPacketID(IQ.nextID());
        connection.sendPacket(transferIQ);
    }
    
    
}
