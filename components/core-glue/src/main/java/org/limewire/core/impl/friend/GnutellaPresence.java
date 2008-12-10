package org.limewire.core.impl.friend;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.io.Address;

/**
 * An implementation of FriendPresence for a Gnutella address.  For example,
 * a GnutellaPresence can be created for a Connection, which is supplied to
 * the RemoteLibraryManager to add and browse the presence.
 */
public class GnutellaPresence implements FriendPresence {

    private final Friend friend;
    private final String id;
    
    /** Map of features supported by this presence. */
    private final Map<URI, Feature> features = new HashMap<URI, Feature>(1);

    /**
     * Constructs a GnutellaPresence with the specified address and id.
     */
    public GnutellaPresence(Address address, String id) {
        this.id = id;
        this.features.put(AddressFeature.ID, new AddressFeature(address));
        this.friend = new GnutellaFriend(address.getAddressDescription(), id, this);
    }
    
    @Override
    public void addFeature(Feature feature) {
        features.put(feature.getID(), feature);
    }

    @Override
    public Feature getFeature(URI id) {
        return features.get(id);
    }

    @Override
    public Collection<Feature> getFeatures() {
        return features.values();
    }

    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return id;
    }

    @Override
    public boolean hasFeatures(URI... id) {
        for (URI uri : id) {
            if (getFeature(uri) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void removeFeature(URI id) {
        features.remove(id);
    }

}
