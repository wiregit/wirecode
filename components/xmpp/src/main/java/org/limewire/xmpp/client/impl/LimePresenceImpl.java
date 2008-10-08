package org.limewire.xmpp.client.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.limewire.io.Address;
import org.limewire.io.InvalidDataException;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.client.impl.messages.address.AddressIQ;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider.ExceptionalAddressIQ;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQ;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQProvider;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.ui.swing.friends.MessageReceivedEvent;
import org.limewire.ui.swing.friends.MessageImpl;
import org.limewire.ui.swing.friends.Message;

import com.google.inject.internal.base.Objects;

public class LimePresenceImpl extends PresenceImpl implements LimePresence {

    private static final Log LOG = LogFactory.getLog(LimePresenceImpl.class);
    
    private Address address;
    private byte [] authToken;
    
    LimePresenceImpl(Presence presence, XMPPConnection connection) {
        super(presence, connection);
    }
    
    LimePresenceImpl(Presence presence, XMPPConnection connection, LimePresence limePresence) {
        super(presence, connection);
        address = Objects.nonNull(limePresence, "limePresence").getPresenceAddress();
    }
    
    @Override
    public Address getPresenceAddress() {
        return address;
    }
    
    @Override
    public String getPresenceId() {
        return getJID();
    }
    
    public void setPresenceAddress(Address address) {
        this.address = address;
    }
    
    public byte [] getAuthToken() {
        return authToken;
    }
    
    public void setAuthToken(byte [] authToken) {
        this.authToken = authToken;
    }
    
    void subscribeAndWaitForAddress() throws InvalidDataException {
        if(LOG.isInfoEnabled()) {
            LOG.info("getting address from " + getJID() + " ...");
        }
        final AddressIQ addressIQ = new AddressIQ();
        addressIQ.setType(IQ.Type.GET);
        addressIQ.setTo(getJID());
        addressIQ.setPacketID(IQ.nextID());
        final PacketCollector addressCollector = connection.createPacketCollector(
            new PacketIDFilter(addressIQ.getPacketID()));         
        connection.sendPacket(addressIQ);
        final AddressIQ response = (AddressIQ) addressCollector.nextResult();
        if (response instanceof ExceptionalAddressIQ) {
            throw new InvalidDataException(((ExceptionalAddressIQ)response).getException());
        }
        address = response.getAddress();
        addressCollector.cancel();
        
        final AuthTokenIQ authTokenIQ = new AuthTokenIQ();
        authTokenIQ.setType(IQ.Type.GET);
        authTokenIQ.setTo(getJID());
        authTokenIQ.setPacketID(IQ.nextID());
        final PacketCollector authTokenCollector = connection.createPacketCollector(
            new PacketIDFilter(authTokenIQ.getPacketID()));         
        connection.sendPacket(authTokenIQ);
        final AuthTokenIQ authTokenResponse = (AuthTokenIQ) authTokenCollector.nextResult();
        if (authTokenResponse instanceof AuthTokenIQProvider.ExceptionalAuthTokenIQ) {
            throw new InvalidDataException(((AuthTokenIQProvider.ExceptionalAuthTokenIQ)authTokenResponse).getException());
        }
        authToken = authTokenResponse.getAuthToken();
        authTokenCollector.cancel();
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

        new MessageReceivedEvent(new MessageImpl(null, null, StringUtils.parseBareAddress(getJID()),
                null, Message.Type.Sent, file)).publish();
    }

}
