package org.limewire.core.impl.library;

import java.beans.PropertyChangeListener;
import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.impl.swing.SwingThreadProxyEventList;

public class FileListAdapter implements LocalFileList, LibraryFileList {

    private final EventList<LocalFileItem> eventList = GlazedLists.threadSafeList(new BasicEventList<LocalFileItem>());
    private final EventList<LocalFileItem> swingEventList = new SwingThreadProxyEventList<LocalFileItem>(eventList);
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public LibraryState getState() {
        return LibraryState.LOADED;
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
        
    }
    
    
    @Override
    public EventList<LocalFileItem> getSwingModel() {
        return swingEventList;
    }
    
    @Override
    public EventList<LocalFileItem> getModel() {
        return eventList;
    }
    
    @Override
    public void addFolder(File folder) {
        
    }
    
    @Override
    public void addFile(File file) {
        eventList.add(new MockLocalFileItem(file.getName(), 1000,12345,23456, 0,0, Category.IMAGE));
    }

    @Override
    public void removeFile(File file) {
        eventList.remove(new MockLocalFileItem(file.getName(), 1000,12345,23456, 0,0, Category.IMAGE));
    }
    
    public void addFileItem(LocalFileItem fileItem) {
        eventList.add(fileItem);
    }
    
    public void removeFileItem(FileItem item) {
        eventList.remove(item);
    }

    @Override
    public int size() {
        return eventList.size();
    }
    
    @Override
    public boolean contains(File file) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean contains(URN urn) {
        // TODO Auto-generated method stub
        return false;
    }
}
