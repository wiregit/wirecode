package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class CloseChatEvent extends AbstractEDTEvent {
    private final Friend friend;
    
    public CloseChatEvent(Friend friend) {
        this.friend = friend;
    }
    
    public Friend getFriend() {
        return friend;
    }
}
