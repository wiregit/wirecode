package org.limewire.core.impl.friend;

import org.limewire.core.api.friend.FriendManager;
import org.limewire.core.api.friend.FriendPresence;

public class MockFriendManager implements FriendManager {

    @Override
    public boolean containsAvailableFriend(String id) {
        return false;
    }

    @Override
    public FriendPresence getMostRelevantFriendPresence(String id) {
        return null;
    }
}
