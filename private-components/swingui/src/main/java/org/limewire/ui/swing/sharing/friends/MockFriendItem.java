package org.limewire.ui.swing.sharing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileList;

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
    public int size() {
        return size;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public FileList getLibrary() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasLibrary() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setLibrary(FileList libraryFileList) {
        // TODO Auto-generated method stub
        
    }

}
