package org.limewire.ui.swing.friends;

class MessageImpl implements Message {
    private final String friendName;
    private final String friendID;
    private final String senderName;
    private final String message;
    private final Type type;
    private final long messageTimeMillis;

    public MessageImpl(String senderName, Friend friend, String message, Type type) {
        this.friendName = friend.getName();
        this.friendID = friend.getID();
        this.senderName = senderName;
        this.message = message;
        this.type = type;
        this.messageTimeMillis = System.currentTimeMillis();
    }
    
    public String getFriendName() {
        return friendName;
    }

    public String getFriendID() {
        return friendID;
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

    public long getMessageTimeMillis() {
        return messageTimeMillis;
    }
}
