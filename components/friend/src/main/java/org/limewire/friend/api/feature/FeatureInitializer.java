package org.limewire.friend.api.feature;

import org.limewire.friend.api.FriendPresence;

import com.google.inject.Inject;

public interface FeatureInitializer {
    @Inject
    void register(FeatureRegistry registry);
    void initializeFeature(FriendPresence friendPresence);
    void removeFeature(FriendPresence friendPresence);
}
