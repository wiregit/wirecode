package org.limewire.xmpp.client.impl.messages.connectrequest;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.ConnectRequestFeature;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectRequestEvent;

/**
 * Listens for {@link ConnectRequestIQ connect request iqs} and fires
 * a {@link ConnectRequestEvent}.
 */
public class ConnectRequestIQListener implements PacketListener {

    private static final Log LOG = LogFactory.getLog(ConnectRequestIQListener.class);
    
    private final EventBroadcaster<ConnectRequestEvent> connectRequestEventBroadcaster;

    public ConnectRequestIQListener(EventBroadcaster<ConnectRequestEvent> connectRequestEventBroadcaster,
                                    FeatureRegistry featureRegistry) {
        this.connectRequestEventBroadcaster = connectRequestEventBroadcaster;
        new ConnectRequestIQFeatureInitializer().register(featureRegistry);
    }
    
    @Override
    public void processPacket(Packet packet) {
        ConnectRequestIQ connectRequest = (ConnectRequestIQ)packet;
        LOG.debugf("processing connect request: {0}", connectRequest);
        connectRequestEventBroadcaster.broadcast(new ConnectRequestEvent(connectRequest.getAddress(), connectRequest.getClientGuid(), connectRequest.getSupportedFWTVersion()));
    }
    
    public PacketFilter getPacketFilter() {
        return new PacketFilter() {
            @Override
            public boolean accept(Packet packet) {
                return packet instanceof ConnectRequestIQ;
            }
        };
    }
    
    private class ConnectRequestIQFeatureInitializer implements FeatureInitializer {
        @Override
        public void register(FeatureRegistry registry) {
            registry.add(ConnectRequestFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            friendPresence.addFeature(new ConnectRequestFeature());
        }
    }

}
