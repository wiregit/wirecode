package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;

import ca.odell.glazedlists.EventList;

/**
 * A {@link RemoteFileList} specifically for a friend.
 */
public interface FriendLibrary extends RemoteFileList {
    /** Returns the friend this library is for. */
    Friend getFriend();
    
//    /** Returns the current state of this friend library. */
//    LibraryState getState();
//    
//    /** Adds a PropertyChangeListener that listens to changes in the state of this library. */
//    void addPropertyChangeListener(PropertyChangeListener listener);
//    /** Removes a PropertyChangeListener that listens to changes in the state of this library. */
//    void removePropertyChangeListener(PropertyChangeListener listener);
    
    /** Returns an EventList of all presence libraries that build up this list. */
    EventList<PresenceLibrary> getPresenceLibraryList();
}
