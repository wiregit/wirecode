package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;
import org.limewire.xmpp.api.client.MessageWriter;

public class ConversationStartedEvent extends AbstractEDTEvent {
    private final Friend friend;
    private final MessageWriter writer;

    ConversationStartedEvent(Friend friend, MessageWriter writer) {
        this.friend = friend;
        this.writer = writer;
    }
    
    public Friend getFriend() {
        return friend;
    }

    public MessageWriter getWriter() {
        return writer;
    }
}
