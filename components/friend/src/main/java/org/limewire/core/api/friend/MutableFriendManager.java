package org.limewire.core.api.friend;

public interface MutableFriendManager extends FriendManager {
    
    void addKnownFriend(Friend friend);

    void removeKnownFriend(Friend user, boolean delete);

    void addAvailableFriend(Friend friend);
    
    void removeAvailableFriend(Friend friend);
}
