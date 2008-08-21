package org.limewire.core.impl.library;

import java.io.File;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FileItem.Category;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

public class FileListAdapter implements FileList {

    private final EventList<FileItem> eventList = GlazedLists.threadSafeList(new BasicEventList<FileItem>());
    private final String name;
    
    public FileListAdapter(String name) {
        this.name = name;
    }
    
    @Override
    public EventList<FileItem> getModel() {
        return eventList;
    }
    
    @Override
    public void addFile(File file) {
        eventList.add(new MockFileItem(file.getName(), 1000,12345,23456, 0,0, Category.IMAGE));
    }

    @Override
    public void removeFile(File file) {
        eventList.remove(new MockFileItem(file.getName(), 1000,12345,23456, 0,0, Category.IMAGE));
    }
    
    public void addFileItem(FileItem fileItem) {
        eventList.add(fileItem);
    }
    
    public void removeFileItem(FileItem item) {
        eventList.remove(item);
    }

    @Override
    public String getName() {
        return name;
    }

}
