package org.limewire.core.api.library;

import java.io.File;

import ca.odell.glazedlists.EventList;

public interface FileList <T extends FileItem> {
    EventList<T> getModel();
    
    public String getName();
    
    public int size();
    
    public void clear();
}
