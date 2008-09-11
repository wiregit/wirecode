package org.limewire.core.api.library;

import java.io.File;

import ca.odell.glazedlists.EventList;

public interface LocalFileList extends FileList<LocalFileItem> {

    EventList<LocalFileItem> getModel();

    public void addFile(File file);
    
    public void removeFile(File file);

}
