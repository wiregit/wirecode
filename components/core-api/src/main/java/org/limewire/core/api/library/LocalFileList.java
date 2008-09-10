package org.limewire.core.api.library;

import java.io.File;

import ca.odell.glazedlists.EventList;

public interface LocalFileList<LocalFileItem> extends FileList {

    EventList<LocalFileItem> getModel();

    public void addFile(File file);
    
    public void removeFile(File file);

}
