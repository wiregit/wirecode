package org.limewire.ui.swing.sharing.friends;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class BuddyItemImpl implements BuddyItem, ListEventListener<FileItem>{

    private final String name;
    private final EventList<FileItem> eventList;
    private FileList buddyLibraryList;
    
    private int size = 0;
    
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    public BuddyItemImpl(String name, EventList<FileItem> eventList) {
        this.name = name;
        this.eventList = eventList;
        this.eventList.addListEventListener(this);
        
        size = eventList.size();
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
//        return size;
        return eventList.size();
    }

    @Override
    public void listChanged(ListEvent<FileItem> listChanges) {
        int newSize = listChanges.getSourceList().size();
        if(newSize != size) {
//        if((newSize > 0 && size == 0) || (newSize == 0 && size > 0) ) {
            int oldSize = size;
            this.size = newSize;
            support.firePropertyChange("size", oldSize, newSize);
        }
    }

    @Override
    public FileList getLibrary() {
        return buddyLibraryList;
    }

    @Override
    public boolean hasLibrary() {
        return buddyLibraryList != null;
    }

    @Override
    public void setLibrary(FileList libraryFileList) {
        this.buddyLibraryList = libraryFileList;
    }
}
