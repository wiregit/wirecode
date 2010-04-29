package org.limewire.core.impl.friend;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.impl.AbstractFriendPresence;

public class MockFriendPresence extends AbstractFriendPresence implements FriendPresence{
    
    private MockFriend friend;
    private String presenceId;
    
    public MockFriendPresence() {
        this(new MockFriend());
    }
    
    public MockFriendPresence(MockFriend friend, Feature...features) {
        this.friend = friend;
    }
    
    public MockFriendPresence(MockFriend friend, String presenceId, Feature...features) {
        this.friend = friend;
        this.presenceId = presenceId;
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return presenceId;
    }
}
