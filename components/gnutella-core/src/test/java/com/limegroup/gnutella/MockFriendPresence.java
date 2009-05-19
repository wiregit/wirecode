package com.limegroup.gnutella;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.impl.AbstractFriendPresence;

public class MockFriendPresence extends AbstractFriendPresence implements FriendPresence {
    
    private MockFriend friend;
    
    public MockFriendPresence() {
        this(new MockFriend());
    }
    
    public MockFriendPresence(MockFriend friend, Feature...features) {
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public String getStatus() {
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public Mode getMode() {
        return null;
    }
}
