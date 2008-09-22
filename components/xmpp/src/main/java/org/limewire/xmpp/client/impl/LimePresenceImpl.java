package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.io.Address;
import org.limewire.io.InvalidDataException;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.client.impl.messages.address.AddressIQ;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider.NullAddressIQ;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;

public class LimePresenceImpl extends PresenceImpl implements LimePresence {

    private static final Log LOG = LogFactory.getLog(LimePresenceImpl.class);
    
    private Address address;
    
    private List<FileMetaData> files;

    LimePresenceImpl(org.jivesoftware.smack.packet.Presence presence, XMPPConnection connection) {
        super(presence, connection);
        files = new ArrayList<FileMetaData>();
    }
    
    public Address getAddress() {
        return address;
    }
    
    public void setAddress(Address address) {
        this.address = address;
    }
    
    void subscribeAndWaitForAddress() throws InvalidDataException {
        if(LOG.isInfoEnabled()) {
            LOG.info("getting address from " + getJID() + " ...");
        }
        final AddressIQ addressIQ = new AddressIQ();
        addressIQ.setType(IQ.Type.GET);
        addressIQ.setTo(getJID());
        addressIQ.setPacketID(IQ.nextID());
        final PacketCollector collector = connection.createPacketCollector(
            new PacketIDFilter(addressIQ.getPacketID()));         
        connection.sendPacket(addressIQ);
        final AddressIQ response = (AddressIQ) collector.nextResult();
        if (response instanceof NullAddressIQ) {
            throw new InvalidDataException(((NullAddressIQ)response).getException());
        }
        address = response.getAddress();
        collector.cancel();
    }

    public void offerFile(FileMetaData file) {
        if(LOG.isInfoEnabled()) {
            LOG.info("offering file " + file.toString() + " to " + getJID());
        }
        final FileTransferIQ transferIQ = new FileTransferIQ(file, FileTransferIQ.TransferType.OFFER);
        transferIQ.setType(IQ.Type.GET);
        transferIQ.setTo(getJID());
        transferIQ.setPacketID(IQ.nextID());
        connection.sendPacket(transferIQ);
    }

    public List<FileMetaData> getFiles() {
        return files;
    }
    
    public void addFile(FileMetaData file) {
        files.add(file);
    }
}
