package org.limewire.ui.swing.search;

import java.util.Collection;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;

/**
 * This interface describes methods that can be invoked from the FromWidget.
 */
public interface RemoteHostActions {
    void chatWith(RemoteHost person);

    void showFilesSharedWith(RemoteHost person);

    void viewLibraryOf(RemoteHost person);
    
    void viewLibraryOf(Friend person);
    
    void viewLibrariesOf(Collection<RemoteHost> people);
}
