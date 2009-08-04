package org.limewire.core.api.library;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;

import ca.odell.glazedlists.EventList;

public interface RemoteLibraryManager {
    
    /**
     * Returns the {@link PresenceLibrary} for this particular
     * {@link FriendPresence}.
     */
    PresenceLibrary getPresenceLibrary(FriendPresence presence);

    /**
     * Adds a new presence to the list of remote libraries. If a presence with
     * the same ID already exists, returns false. If the presence is the first
     * with that particular friend, a FriendLibrary is created and returns true.
     */
    boolean addPresenceLibrary(FriendPresence presence);
    
    /**
     * Removes a presence from the list of presence libraries
     * for the given friend.  If this is the last live presence
     * for the given friend, the friend is removed from the list
     * of friend libraries.
     */
    void removePresenceLibrary(FriendPresence presence);
    
    /**
     * Removes the given the entire FriendLibrary for the given Friend,
     * including all contained PresenceLibraries.
     */
    void removeFriendLibrary(Friend friend);
    
    /**
     * Returns an {@link EventList} composed of {@link FriendLibrary FriendLibraries}.
     */
    EventList<FriendLibrary> getFriendLibraryList();
    
    /**
     * Returns true if a {@link FriendLibrary} exists for the given friend.
     */
    boolean hasFriendLibrary(Friend friend);

    /** A list of all friends' libraries suitable for use in Swing. */
    EventList<FriendLibrary> getSwingFriendLibraryList();
    
    /** Returns a SearchResultList that is a concatenation of all friends libraries. */
    SearchResultList getAllFriendsFileList();
    
    /** 
     * Returns the FriendLibrary for this friend if one exists, null 
     * if one does not exist. 
     */
    FriendLibrary getFriendLibrary(Friend friend);
}
