package org.limewire.core.api.library;

import java.util.Collection;

import org.limewire.core.api.friend.Friend;

/**
 * Library Management of a gnutella share lists & friend share lists, and remote friend libraries.
 */
public interface LibraryManager {

    void addLibraryLisListener(LibraryListListener libraryListener);

    void removeLibraryListener(LibraryListListener libraryListener);

    LocalFileList getLibraryManagedList();

    LocalFileList getGnutellaShareList();
    
    Collection<LocalFileList> getAllFriendShareLists(); 

    LocalFileList getOrCreateFriendShareList(Friend friend);
    
    LocalFileList getFriendShareList(Friend friend);

    void removeFriendShareList(Friend friend);

    RemoteFileList getOrCreateFriendLibrary(Friend friend);

    void removeFriendLibrary(Friend friend);
}
