package org.limewire.core.api.friend.feature.features;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.listener.EventBroadcaster;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.ConnectBackRequestedEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ConnectBackRequestFeatureTransportHandler implements FeatureTransport.Handler<ConnectBackRequest>{
    private final EventBroadcaster<ConnectBackRequestedEvent> connectBackRequestedEventBroadcaster;

    @Inject
    public ConnectBackRequestFeatureTransportHandler(FeatureRegistry featureRegistry,
                                                     EventBroadcaster<ConnectBackRequestedEvent> connectBackRequestedEventBroadcaster) {
        this.connectBackRequestedEventBroadcaster = connectBackRequestedEventBroadcaster;
        new ConnectBackRequestIQFeatureInitializer().register(featureRegistry);
    }

    @Override
    public void featureReceived(String from, ConnectBackRequest connectBackRequest) {
        connectBackRequestedEventBroadcaster.broadcast(new ConnectBackRequestedEvent(connectBackRequest));
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
