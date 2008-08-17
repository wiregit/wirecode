package org.limewire.ui.swing.friends;

class MessageImpl implements Message {
    private final Friend friend;
    private final String senderName;
    private final String message;
    private final Type type;

    public MessageImpl(String senderName, Friend friend, String message, Type type) {
        this.friend = friend;
        this.senderName = senderName;
        this.message = message;
        this.type = type;
    }
    
    public Friend getFriend() {
        return friend;
    }

    public String getMessageText() {
        return message;
    }

    public String getSenderName() {
        return senderName;
    }

    public Type getType() {
        return type;
    }
}
