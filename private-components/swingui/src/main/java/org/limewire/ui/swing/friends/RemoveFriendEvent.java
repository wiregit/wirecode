package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class RemoveFriendEvent extends AbstractEDTEvent {
    private final Friend friend;

    public RemoveFriendEvent(Friend friend) {
        super();
        this.friend = friend;
    }

    public Friend getFriend() {
        return friend;
    }
}
