package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

public interface RemoteFileList extends FileList<RemoteFileItem> {
    
    public EventList<RemoteFileItem> getSwingModel();
    
    public void addFile(RemoteFileItem file);
    
    public void removeFile(RemoteFileItem file);
}
