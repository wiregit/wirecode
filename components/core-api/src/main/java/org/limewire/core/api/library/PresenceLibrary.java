package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.friend.FriendPresence;

public interface PresenceLibrary extends RemoteFileList {
    FriendPresence getPresence();

    /** Returns the current state of this presence library. */
    LibraryState getState();
    
    /** Sets the current state. */
    void setState(LibraryState newState);
    
    void addPropertyChangeListener(PropertyChangeListener listener);
    void removePropertyChangeListener(PropertyChangeListener listener);
}
