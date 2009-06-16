package org.limewire.xmpp.client.impl.messages.authtoken;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.core.api.friend.feature.features.AuthToken;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

@Singleton
public class AuthTokenIQListener implements PacketListener, FeatureTransport<AuthToken> {

    private final XMPPFriendConnectionImpl connection;
    private final FeatureTransport.Handler<AuthToken> handler;

    @AssistedInject
    public AuthTokenIQListener(@Assisted XMPPFriendConnectionImpl connection,
                               FeatureTransport.Handler<AuthToken> handler) {
        this.connection = connection;
        this.handler = handler;
    }

    public void processPacket(Packet packet) {
        final AuthTokenIQ iq = (AuthTokenIQ)packet;
        if(iq.getType().equals(IQ.Type.SET)) {
            handler.featureReceived(iq.getFrom(), iq.getAuthToken());

        }
    }

    @Override
    public void sendFeature(FriendPresence presence, AuthToken localFeature) throws FriendException {
        AuthTokenIQ queryResult = new AuthTokenIQ(localFeature);
        queryResult.setTo(presence.getPresenceId());
        queryResult.setFrom(connection.getLocalJid());
        queryResult.setType(IQ.Type.SET);
        connection.sendPacket(queryResult);
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof AuthTokenIQ;
            }
        };
    }
}