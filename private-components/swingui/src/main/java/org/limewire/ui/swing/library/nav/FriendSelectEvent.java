package org.limewire.ui.swing.library.nav;

import org.limewire.core.api.friend.Friend;

public class FriendSelectEvent {

    private final Friend friend;

    public FriendSelectEvent(Friend friend) {
        this.friend = friend;
    }

    /**
     * Returns the currently selected friend triggering this event. It may be
     * null if the friend has become unselected.
     */
    public Friend getSelectedFriend() {
        return friend;
    }
}
