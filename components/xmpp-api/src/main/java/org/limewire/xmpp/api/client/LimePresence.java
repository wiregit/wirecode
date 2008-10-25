package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.FriendPresence;

/**
 * Marks a presence as running in limewire.  Allows for additional limewire
 * specific features.
 */
public interface LimePresence extends Presence, FriendPresence {

    /**
     * offer a file to this user; blocking call.
     * @param file
     */
    public void offerFile(FileMetaData file);

    /** Returns the containing user. */
    User getUser();

    void sendLibraryRefresh();
}
