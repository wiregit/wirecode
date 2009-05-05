package org.limewire.core.api.friend.feature;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;

public interface FeatureTransport <T> {
    void sendFeature(FriendPresence presence, T localFeature) throws FriendException;
    //void setHandler(Handler handler);
    interface Handler <T> {
        void featureReceived(String from, T feature);
    }
}
