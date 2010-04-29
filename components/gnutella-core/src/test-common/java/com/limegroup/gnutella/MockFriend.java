package com.limegroup.gnutella;

import org.limewire.friend.api.Friend;

public class MockFriend implements Friend {
    
    private String localID;

    public MockFriend() {
        this(null);
    }
    
    public MockFriend(String localID) {
        this.localID = localID;
    }
    
    @Override
    public String getId() {
        return localID;
    }

    @Override
    public String getName() {
        return localID;
    }

    @Override
    public String getRenderName() {
        return  localID;
    }
    
    public String getFirstName() {
        return null;
    }
}
