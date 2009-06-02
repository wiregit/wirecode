package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

/**
 * Manager for all share lists.
 */
public interface SharedFileListManager {

    /**
     * Returns an EventList that can be used to view all {@link SharedFileList
     * SharedFileLists}.
     */
    EventList<SharedFileList> getModel();

    /**
     * Creates a new {@link SharedFileList} of the appropriate name. The new
     * list will be reflected in {@link #getModel()} once the core processes it.
     */
    void createNewSharedFileList(String name);

    // TODO: getSharedFileList(String name), or (int id) ?
}
