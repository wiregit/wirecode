package org.limewire.friend.impl;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;

/**
 * Abstract implementation of {@link FriendPresence} providing
 * management of features.
 */
public abstract class AbstractFriendPresence implements FriendPresence {
    
    private final Map<URI, Feature> features;
    
    public AbstractFriendPresence() {
        this.features = new ConcurrentHashMap<URI, Feature>(5, 0.75f, 1);
    }

    @Override
    public Feature getFeature(URI id) {
        return features.get(id);
    }

    @Override
    public boolean hasFeatures(URI... id) {
        for(URI uri : id) {
            if(getFeature(uri) == null) {
                return false;
            }
        }
        return true;
    }
}
