package org.limewire.ui.swing.sharing.friends;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class BuddyItemImpl implements BuddyItem, ListEventListener<FileItem>{

    private final String name;
    private final EventList<FileItem> eventList;
    
    private int size = 0;
    
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    public BuddyItemImpl(String name, EventList<FileItem> eventList) {
        this.name = name;
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
    public String getName() {
        return name;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void listChanged(ListEvent<FileItem> listChanges) {
        int newSize = listChanges.getSourceList().size();
        if(newSize != size) {
            int oldSize = size;
            this.size = newSize;
            support.firePropertyChange("size", oldSize, newSize);
        }
    }
}
