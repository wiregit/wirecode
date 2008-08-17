package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class MessageReceivedEvent extends AbstractEDTEvent {
    private final Message message;

    public MessageReceivedEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
    
    @Override
    public void publish() {
        super.publish(message.getFriend().getName());
    }
}
