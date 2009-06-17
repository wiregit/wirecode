package com.limegroup.gnutella;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;

public class MockFriendPresence implements FriendPresence {
    
    private MockFriend friend;
    private Map<URI, Feature> features;
    private String presenceId;
    
    public MockFriendPresence() {
        this(new MockFriend(), null);
    }
    
    public MockFriendPresence(MockFriend friend, String presenceId, Feature...features) {
        this.presenceId = presenceId;
        this.features = new ConcurrentHashMap<URI, Feature>();
        this.friend = friend;
        for(Feature feature : features) {
            addFeature(feature);
        }
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return presenceId;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return features.values();
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

    @Override
    public void addFeature(Feature feature) {
        features.put(feature.getID(), feature);
    }

    @Override
    public void removeFeature(URI id) {
        features.remove(id);
    }
}
