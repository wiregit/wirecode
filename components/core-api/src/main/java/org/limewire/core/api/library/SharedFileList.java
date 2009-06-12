package org.limewire.core.api.library;

import com.limegroup.gnutella.library.SharedFileCollection;

import ca.odell.glazedlists.EventList;

/** The core API analog to {@link SharedFileCollection}. */
public interface SharedFileList extends LocalFileList {
    
    /** Returns all friend IDs this list is shared with. */
    EventList<String> getFriendIds();
    
    /** Adds a new friend id to share this list with. */
    void addFriend(String friendId);
    
    /** Removes a friend id from the list of friends this is shared with. */
    void removeFriend(String friendId);
    
    /** Returns the name of this collection. */
    String getCollectionName();
    
    /** Sets a new name for this collection. */
    void setCollectionName(String name);
    
    /** Returns true if name changing is allowed for this collection. */
    boolean isNameChangeAllowed();

}