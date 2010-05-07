package org.limewire.core.api.library;

import ca.odell.glazedlists.EventList;

/** The core API analog to {@link com.limegroup.gnutella.library.SharedFileCollection}. */
public interface SharedFileList extends LocalFileList {
    
    /** Returns all friend IDs this list is shared with. */
    EventList<String> getFriendIds();
    
    /** Returns the name of this collection. */
    String getCollectionName();
    
    /** Returns true if this is a public share list. */
    boolean isPublic();
    
    /** Returns a unique identifier for this list. */
    int getId();
}
