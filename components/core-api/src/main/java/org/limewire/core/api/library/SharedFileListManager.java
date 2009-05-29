package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

/**
 * Manager for all share lists.
 */
public interface SharedFileListManager {
    
    EventList<SharedFileList> getModel();
    
    SharedFileList createNewSharedFileList(String name);
    
    // TODO: getSharedFileList(String name), or (int id) ?
}
