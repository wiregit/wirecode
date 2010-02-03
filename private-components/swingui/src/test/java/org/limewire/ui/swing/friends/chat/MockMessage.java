package org.limewire.ui.swing.friends.chat;

public class MockMessage implements MessageText {
    private final String friendID;
    private final String senderName;
    private final String message;
    private final Type type;
    private final long messageTimeMillis;

    public MockMessage(ChatFriend chatFriend, String message, long messageTimeMillis, String senderName, Type type) {
        this.friendID = chatFriend.getID();
        this.message = message;
        this.messageTimeMillis = messageTimeMillis;
        this.senderName = senderName;
        this.type = type;
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
    public String format() {
        return message;
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
