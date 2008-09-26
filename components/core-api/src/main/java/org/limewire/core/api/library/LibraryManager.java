package org.limewire.core.api.library;

import java.util.Map;

import org.limewire.core.api.friend.Friend;

public interface LibraryManager {

    void addLibraryLisListener(LibraryListListener libraryListener);

    void removeLibraryListener(LibraryListListener libraryListener);

    LocalFileList getLibraryList();

    LocalFileList getGnutellaList();

    Map<String, LocalFileList> getAllBuddyLists();

    LocalFileList getBuddy(String id);

    void addBuddy(String id);

    void removeBuddy(String id);

    boolean containsBuddy(String id);

    RemoteFileList getOrCreateBuddyLibrary(Friend friend);

    void removeBuddyLibrary(Friend friend);
}
