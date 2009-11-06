package org.limewire.promotion.search;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;

/**
 * An implementation of FriendPresence for the Lime Store.
 */
class StorePresence implements FriendPresence {

    private final Friend friend;
    private final String id;
    
    /**
     * Constructs a StorePresence with the specified id.
     */
    public StorePresence(String id) {
        this.id = id;
        this.friend = new StoreFriend(this);
    }
    
    @Override
    public void addFeature(Feature feature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFeature(URI id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasFeatures(URI... id) {
        return false;
    }

    @Override
    public Feature getFeature(URI id) {
        return null;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return Collections.emptySet();
    }

    @Override
    public <D, F extends Feature<D>> void addTransport(Class<F> clazz, FeatureTransport<D> transport) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <F extends Feature<D>, D> FeatureTransport<D> getTransport(Class<F> feature) {
        return null;
    }

    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public Mode getMode() {
        return Mode.available;
    }

    @Override
    public String getPresenceId() {
        return id;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getStatus() {
        return "";
    }

    @Override
    public Type getType() {
        return Type.available;
    }
}
