package org.limewire.ui.swing.sharing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.friend.Friend;

public class MockFriendItem implements FriendItem {

    private Friend friend;
    private int size;
    
    public MockFriendItem(Friend friend, int size) {
        this.friend = friend;
        this.size = size;
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public int getShareListSize() {
        return size;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }

}
