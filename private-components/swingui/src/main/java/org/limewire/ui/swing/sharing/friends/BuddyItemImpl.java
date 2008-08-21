package org.limewire.ui.swing.sharing.friends;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;

public class BuddyItemImpl implements BuddyItem {

    private final String name;
    private final EventList<FileItem> eventList;
    private boolean isOnline;
    
    public BuddyItemImpl(String name, EventList<FileItem> eventList) {
        this.name = name;
        this.eventList = eventList;
        isOnline = false;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isOnline() {
        return isOnline;
    }

    @Override
    public int size() {
        return eventList.size();
    }

    public void setOnline(boolean value) {
        this.isOnline = value;
    }
}
