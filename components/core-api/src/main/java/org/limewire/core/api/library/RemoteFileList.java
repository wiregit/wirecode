package org.limewire.core.api.library;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.EventList;

public interface RemoteFileList extends FileList<RemoteFileItem> {
    
    public EventList<RemoteFileItem> getModel();
    
    public void addFile(SearchResult file);
    
    public void removeFile(SearchResult file);
}
