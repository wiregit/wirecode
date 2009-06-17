package org.limewire.core.api.friend.feature.features;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FileOffer;
import org.limewire.core.api.friend.client.FileOfferEvent;
import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileOfferFeatureTransportHandler implements FeatureTransport.Handler<FileMetaData>{

    private final EventBroadcaster<FileOfferEvent> fileOfferBroadcaster;

    @Inject
    public FileOfferFeatureTransportHandler(FeatureRegistry featureRegistry,
                                            EventBroadcaster<FileOfferEvent> fileOfferBroadcaster) {
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        new FileOfferInitializer().register(featureRegistry);
    }

    @Override
    public void featureReceived(String from, FileMetaData feature) {
        // TODO async?
        fileOfferBroadcaster.broadcast(new FileOfferEvent(new FileOffer(feature, from), FileOfferEvent.Type.OFFER));
        // TODO send acceptance or rejection;
        // TODO only needed for user feedback

    }

    private class FileOfferInitializer implements FeatureInitializer {

        @Override
        public void register(FeatureRegistry registry) {
            registry.add(FileOfferFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            friendPresence.addFeature(new FileOfferFeature());
        }


        @Override
        public void removeFeature(FriendPresence friendPresence) {
            friendPresence.removeFeature(FileOfferFeature.ID);
        }
    }
}
