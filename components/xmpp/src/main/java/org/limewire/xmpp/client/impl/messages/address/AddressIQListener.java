package org.limewire.xmpp.client.impl.messages.address;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.client.impl.XMPPConnectionImpl;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

@Singleton
public class AddressIQListener implements PacketListener, FeatureTransport<Address> {
    private static final Log LOG = LogFactory.getLog(AddressIQListener.class);

    private final XMPPConnectionImpl connection;
    private final AddressFactory factory;
    private final Handler<Address> handler;

    @AssistedInject
    public AddressIQListener(@Assisted XMPPConnectionImpl connection,
                             @Assisted AddressFactory factory,
                             Handler<Address> handler) {
        this.connection = connection;
        this.factory = factory;
        this.handler = handler;
    }

    public void processPacket(Packet packet) {
        AddressIQ iq = (AddressIQ)packet;
        if(iq.getType().equals(IQ.Type.SET)) {
            synchronized (this) {
                handler.featureReceived(iq.getFrom(), iq.getAddress());
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

    public void sendFeature(FriendPresence presence, Address address) throws FriendException {
        LOG.debugf("sending new address to {0}", presence.getPresenceId());
        AddressIQ queryResult = new AddressIQ(address, factory);
        queryResult.setTo(presence.getPresenceId());
        queryResult.setFrom(connection.getLocalJid());
        queryResult.setType(IQ.Type.SET);
        connection.sendPacket(queryResult);
    }
}