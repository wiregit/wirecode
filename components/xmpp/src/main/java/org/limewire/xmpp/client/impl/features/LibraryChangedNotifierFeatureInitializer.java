package org.limewire.xmpp.client.impl.features;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifierFeature;

public class LibraryChangedNotifierFeatureInitializer implements FeatureInitializer {

    @Override
    public void register(FeatureRegistry registry) {
        registry.add(LibraryChangedNotifierFeature.ID, this);
    }

    @Override
    public void initializeFeature(FriendPresence friendPresence) {
        friendPresence.addFeature(new LibraryChangedNotifierFeature(new LibraryChangedNotifier(){}));
    }

    @Override
    public void removeFeature(FriendPresence friendPresence) {
        friendPresence.removeFeature(LibraryChangedNotifierFeature.ID);
    }
}
