package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

public interface BuddyFileList extends FileList {

    public void setFilteredModel(EventList<FileItem> filteredList);
    
    public EventList<FileItem> getFilteredModel();
    
    public int getFilteredSize();
}
