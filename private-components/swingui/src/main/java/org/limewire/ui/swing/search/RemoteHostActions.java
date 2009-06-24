package org.limewire.ui.swing.search;

import java.util.Collection;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.friend.api.Friend;

/**
 * This interface describes methods that can be invoked from the FromWidget.
 */
public interface RemoteHostActions {
    void chatWith(RemoteHost person);

    void showFilesSharedWith(RemoteHost person);
    /**
     * @param person Can be anonymous but not null.
     */
    void viewLibraryOf(RemoteHost person);
    
    /**
     * @param person Can not be anonymous or null.
     */
    void viewLibraryOf(Friend person);
    
    /**
     * @param people Can be anonymous but not null.
     */
    void viewLibrariesOf(Collection<RemoteHost> people);

    /**Shows files from all friends*/
    void browseAllFriends();
}
