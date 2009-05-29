package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;

import ca.odell.glazedlists.EventList;

public interface SharedFileList extends LocalFileList {
    
    EventList<Friend> getFriends();
    
    void addFriend(Friend friend);
    
    void removeFriend(Friend friend);
    
    String getName();
    
    void setName(String name);
    
    boolean isNameChangeAllowed();

}
