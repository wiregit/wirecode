package org.limewire.ui.swing.friends;

public class MockMessage implements Message {
    private final Friend friend;
    private final String senderName;
    private final String message;
    private final Type type;
    private final long messageTimeMillis;
    
    public MockMessage(Friend friend, String message, long messageTimeMillis, String senderName,
            Type type) {
        this.friend = friend;
        this.message = message;
        this.messageTimeMillis = messageTimeMillis;
        this.senderName = senderName;
        this.type = type;
    }

    @Override
    public Friend getFriend() {
        return friend;
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
