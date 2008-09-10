package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

public interface BuddyFileList extends LocalFileList {

    public void setFilteredModel(EventList<LocalFileItem> filteredList);
    
    public EventList<LocalFileItem> getFilteredModel();
    
    public int getFilteredSize();
}
