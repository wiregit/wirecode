package org.limewire.ui.swing.sharing.friends;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class FriendItemImpl implements FriendItem, ListEventListener<LocalFileItem> {

    private final Friend friend;
    private final EventList<LocalFileItem> eventList;
    
    private int size = 0;
    
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    public FriendItemImpl(Friend friend, EventList<LocalFileItem> eventList) {
        this.friend = friend;
        this.eventList = eventList;
        this.eventList.addListEventListener(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public int getShareListSize() {
        return eventList.size();
    }

    @Override
    public void listChanged(ListEvent<LocalFileItem> listChanges) {
        int newSize = listChanges.getSourceList().size();
        if(newSize != size) {
            int oldSize = size;
            this.size = newSize;
            support.firePropertyChange("shareListSize", oldSize, newSize);
        }
    }
}
