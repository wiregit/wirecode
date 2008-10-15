package org.limewire.ui.swing.sharing.friends;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.friend.Friend;

/**
 * Item to display in a list of names for friends you are sharing with. 
 * When the number of items you are sharing with this friend, a 
 * property change event is fired.
 */
public interface FriendItem {
    
    public int getShareListSize();
    
    public Friend getFriend();
    
    public void addPropertyChangeListener(PropertyChangeListener l);
    
    public void removePropertyChangeListener(PropertyChangeListener l);
}
