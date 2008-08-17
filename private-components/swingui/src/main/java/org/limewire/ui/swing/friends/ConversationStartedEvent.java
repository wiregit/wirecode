package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class ConversationStartedEvent extends AbstractEDTEvent {
    private final Friend friend;

    ConversationStartedEvent(Friend friend) {
        this.friend = friend;
    }
    
    public Friend getFriend() {
        return friend;
    }
}
