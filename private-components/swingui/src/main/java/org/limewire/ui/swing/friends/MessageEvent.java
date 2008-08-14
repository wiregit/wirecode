package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class MessageEvent extends AbstractEDTEvent implements Message {
    private final String friend;
    private final String message;
    private final Type type;

    public MessageEvent(String friend, String message, Type type) {
        this.friend = friend;
        this.message = message;
        this.type = type;
    }

    public String getMessageText() {
        return message;
    }

    public String getSenderName() {
        return friend;
    }

    public Type getType() {
        return type;
    }
}
