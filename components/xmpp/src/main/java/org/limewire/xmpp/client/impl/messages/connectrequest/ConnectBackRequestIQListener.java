package org.limewire.xmpp.client.impl.messages.connectrequest;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.core.api.friend.feature.features.ConnectBackRequestFeature;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.ConnectBackRequestedEvent;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Listens for {@link ConnectBackRequestIQ connect back request iqs} and fires
 * a {@link ConnectBackRequestedEvent}.
 */
public class ConnectBackRequestIQListener implements PacketListener, FeatureTransport<ConnectBackRequest> {

    private static final Log LOG = LogFactory.getLog(ConnectBackRequestIQListener.class);
    
    private final EventBroadcaster<ConnectBackRequestedEvent> connectBackRequestedEventBroadcaster;

    private final XMPPFriendConnectionImpl connection;

    @AssistedInject
    public ConnectBackRequestIQListener(@Assisted XMPPFriendConnectionImpl connection,
            EventBroadcaster<ConnectBackRequestedEvent> connectBackRequestedEventBroadcaster,
            FeatureRegistry featureRegistry) {
        this.connection = connection;
        this.connectBackRequestedEventBroadcaster = connectBackRequestedEventBroadcaster;
        new ConnectBackRequestIQFeatureInitializer().register(featureRegistry);
    }
    
    @Override
    public void processPacket(Packet packet) {
        ConnectBackRequestIQ connectRequest = (ConnectBackRequestIQ)packet;
        LOG.debugf("processing connect request: {0}", connectRequest);
        connectBackRequestedEventBroadcaster.broadcast(new ConnectBackRequestedEvent(connectRequest.getConnectBackRequest()));
    }
    
    @Override
    public void sendFeature(FriendPresence presence, ConnectBackRequest connectBackRequest)
            throws FriendException {
        ConnectBackRequestIQ connectRequest = new ConnectBackRequestIQ(connectBackRequest);
        connectRequest.setTo(presence.getPresenceId());
        try {
            connectRequest.setFrom(connection.getLocalJid());
            LOG.debugf("sending request: {0}", connectRequest);
            connection.sendPacket(connectRequest);
        } catch (FriendException e) {
            LOG.debug("sending connect back request failed", e);
        }
    }
    
    public PacketFilter getPacketFilter() {
        return new PacketFilter() {
            @Override
            public boolean accept(Packet packet) {
                return packet instanceof ConnectBackRequestIQ;
            }
        };
    }
    
    private class ConnectBackRequestIQFeatureInitializer implements FeatureInitializer {
        @Override
        public void register(FeatureRegistry registry) {
            registry.add(ConnectBackRequestFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            friendPresence.addFeature(new ConnectBackRequestFeature());
        }

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(ConnectBackRequestFeature.ID);
        }
    }

}
