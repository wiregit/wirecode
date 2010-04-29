package org.limewire.ui.swing.friends;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.impl.AbstractFriendPresence;

public class MockFriendPresence extends AbstractFriendPresence implements FriendPresence{
    
    private MockFriend friend;
    
    public MockFriendPresence() {
        this(new MockFriend());
    }
    
    public MockFriendPresence(MockFriend friend, Feature...features) {
        this.friend = friend;
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return null;
    }
}
