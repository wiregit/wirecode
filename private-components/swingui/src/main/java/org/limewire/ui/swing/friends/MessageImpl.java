package org.limewire.ui.swing.friends;

class MessageImpl implements Message {
    private final String friend;
    private final String message;
    private final Type type;

    public MessageImpl(String friend, String message, Type type) {
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
