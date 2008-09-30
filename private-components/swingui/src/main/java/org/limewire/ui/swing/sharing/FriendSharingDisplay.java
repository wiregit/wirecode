package org.limewire.ui.swing.sharing;

import org.limewire.core.api.friend.Friend;

public interface FriendSharingDisplay {

    /**
     * Responsible for displaying the "All Friends" friend sharing panel in the main view of the application
     */
    public void displaySharing();

    /**
     * Displays the "All Friends" friend sharing panel, and selects the friend.
     */
    public void selectFriendInFileSharingList(Friend friend);
    
    /**
     * Displays the library for the supplied friend.
     */
    public void selectFriendLibrary(Friend friend);
}
