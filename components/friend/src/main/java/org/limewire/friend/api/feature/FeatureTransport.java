package org.limewire.friend.api.feature;

import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;

public interface FeatureTransport <T> {
    void sendFeature(FriendPresence presence, T localFeature) throws FriendException;
    //void setHandler(Handler handler);
    interface Handler <T> {
        void featureReceived(String from, T feature);
    }
}
