package com.limegroup.gnutella;

import java.util.LinkedHashMap;
import java.util.Map;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;

public class MockFriend implements Friend {
    
    private String localID;
    private boolean anonymous;
    private Map<String, FriendPresence> friendPresences; 

    public MockFriend() {
        this(null);
    }
    
    public MockFriend(String localID) {
        this(localID, true);
    }
    
    public MockFriend(String localID, boolean anonymous) {
        this.localID = localID;
        this.anonymous = anonymous;
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

    @Override
    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public Network getNetwork() {
        return new Network() {
            @Override
            public String getCanonicalizedLocalID() {
                return localID;
            }

            @Override
            public String getNetworkName() {
                return "";
            }
        };
    }

    @Override
    public Map<String, FriendPresence> getFriendPresences() {
        return friendPresences;
    }
    
    public void setFriendPresences(Map<String, FriendPresence> presences) {
        this.friendPresences = presences;
    }
    
    public void addPresence(FriendPresence presence) {
        if(friendPresences == null) {
            // use linked to make iterating easier for tests.
            friendPresences = new LinkedHashMap<String, FriendPresence>();
        }
        friendPresences.put(presence.getPresenceId(), presence);
    }

    @Override
    public String getFirstName() {
        return null;
    }
}
