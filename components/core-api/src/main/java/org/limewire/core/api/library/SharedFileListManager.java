package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

/**
 * Manager for all share lists.
 */
public interface SharedFileListManager {
    
    EventList<SharedFileList> getModel();
    
    void createNewSharedFileList(String name);
    
    // TODO: getSharedFileList(String name), or (int id) ?
}
