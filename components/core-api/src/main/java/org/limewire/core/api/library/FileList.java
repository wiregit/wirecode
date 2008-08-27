package org.limewire.core.api.library;

import java.io.File;

import ca.odell.glazedlists.EventList;

public interface FileList {

    public EventList<FileItem> getModel();
    
    public void addFile(File file);
    
    public void removeFile(File file);
    
    public String getName();
    
    public int size();
}
