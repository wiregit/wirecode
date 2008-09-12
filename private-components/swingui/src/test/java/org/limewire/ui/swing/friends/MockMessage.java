package org.limewire.ui.swing.friends;

public class MockMessage implements Message {
    private final String friendName;
    private final String friendID;
    private final String senderName;
    private final String message;
    private final Type type;
    private final long messageTimeMillis;
    
    public MockMessage(Friend friend, String message, long messageTimeMillis, String senderName,
            Type type) {
        this.friendName = friend.getName();
        this.friendID = friend.getID();
        this.message = message;
        this.messageTimeMillis = messageTimeMillis;
        this.senderName = senderName;
        this.type = type;
    }

    @Override
    public String getFriendName() {
        return friendName;
    }

    @Override
    public String getFriendID() {
        return friendID;
    }

    @Override
    public String getMessageText() {
        return message;
    }

    @Override
    public long getMessageTimeMillis() {
        return messageTimeMillis;
    }

    @Override
    public String getSenderName() {
        return senderName;
    }

    @Override
    public Type getType() {
        return type;
    }
}
