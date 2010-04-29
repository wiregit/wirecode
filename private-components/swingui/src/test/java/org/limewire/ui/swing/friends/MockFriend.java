package org.limewire.ui.swing.friends;

import org.limewire.friend.api.Friend;

public class MockFriend implements Friend {
    
    private String localID;
    private String name;
    public MockFriend() {
        this(null);
    }
    
    public MockFriend(String localID) {
        this.localID = localID;
        this.name = localID;
    }
    
    @Override
    public String getId() {
        return localID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRenderName() {
        return getName();
    }
}
