package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;

import org.limewire.xmpp.api.client.LimePresence;

public interface PresenceLibrary extends RemoteFileList {
    LimePresence getPresence();

    /** Returns the current state of this presence library. */
    LibraryState getState();
    
    /** Sets the current state. */
    void setState(LibraryState newState);
    
    void addPropertyChangeListener(PropertyChangeListener listener);
    void removePropertyChangeListener(PropertyChangeListener listener);
}
