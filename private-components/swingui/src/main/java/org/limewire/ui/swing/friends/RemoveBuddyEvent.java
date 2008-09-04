package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class RemoveBuddyEvent extends AbstractEDTEvent {
    private final Friend friend;

    public RemoveBuddyEvent(Friend friend) {
        super();
        this.friend = friend;
    }

    public Friend getFriend() {
        return friend;
    }
}
