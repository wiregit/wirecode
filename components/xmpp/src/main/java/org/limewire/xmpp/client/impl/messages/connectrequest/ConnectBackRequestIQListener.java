package org.limewire.xmpp.client.impl.messages.connectrequest;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.ConnectBackRequestFeature;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequestEvent;

/**
 * Listens for {@link ConnectBackRequestIQ connect back request iqs} and fires
 * a {@link ConnectBackRequestEvent}.
 */
public class ConnectBackRequestIQListener implements PacketListener {

    private static final Log LOG = LogFactory.getLog(ConnectBackRequestIQListener.class);
    
    private final EventBroadcaster<ConnectBackRequestEvent> connectBackRequestEventBroadcaster;

    public ConnectBackRequestIQListener(EventBroadcaster<ConnectBackRequestEvent> connectRequestEventBroadcaster,
                                    FeatureRegistry featureRegistry) {
        this.connectBackRequestEventBroadcaster = connectRequestEventBroadcaster;
        new ConnectBackRequestIQFeatureInitializer().register(featureRegistry);
    }
    
    @Override
    public void processPacket(Packet packet) {
        ConnectBackRequestIQ connectRequest = (ConnectBackRequestIQ)packet;
        LOG.debugf("processing connect request: {0}", connectRequest);
        connectBackRequestEventBroadcaster.broadcast(new ConnectBackRequestEvent(connectRequest.getAddress(), connectRequest.getClientGuid(), connectRequest.getSupportedFWTVersion()));
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
    }

}
