package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

class FriendLoginEvent extends AbstractEDTEvent {
    private final Friend friend;
    
    public FriendLoginEvent(Friend friend) {
        this.friend = friend;
    }

    public Friend getFriend() {
        return friend;
    }
}
