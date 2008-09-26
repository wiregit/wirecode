package org.limewire.core.api.library;

import java.util.Map;

import org.limewire.core.api.friend.Friend;

public interface LibraryManager {

    void addLibraryLisListener(LibraryListListener libraryListener);

    void removeLibraryListener(LibraryListListener libraryListener);

    LocalFileList getLibraryList();

    LocalFileList getGnutellaList();

    Map<String, LocalFileList> getAllFriendLists();

    LocalFileList getFriend(String id);

    void addFriend(String id);

    void removeFriend(String id);

    boolean containsFriend(String id);

    RemoteFileList getOrCreateFriendLibrary(Friend friend);

    void removeFriendLibrary(Friend friend);
}
