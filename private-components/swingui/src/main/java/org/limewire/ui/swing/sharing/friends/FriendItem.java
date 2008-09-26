package org.limewire.ui.swing.sharing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileList;

public interface FriendItem {
    
    public int size();
    
    public Friend getFriend();
    
    public void addPropertyChangeListener(PropertyChangeListener l);
    
    public void removePropertyChangeListener(PropertyChangeListener l);
    
    public boolean hasLibrary();
    
    public void setLibrary(FileList libraryFileList);
    
    public FileList getLibrary();
}
