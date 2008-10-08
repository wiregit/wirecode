package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.FileMetaData;

public class MessageImpl implements Message {
    private final String friendName;
    private final String friendID;
    private final String senderName;
    private final String message;
    private final Type type;
    private final long messageTimeMillis;
    private final FileMetaData fileOffer;

    public MessageImpl(String senderName, ChatFriend chatFriend, String message, Type type) {
        this(senderName, chatFriend.getName(), chatFriend.getID(), message, type, null);
    }

    public MessageImpl(String senderName, String friendName, String friendId, String message, Type type, FileMetaData fileMetaData) {
        this.friendName = friendName;
        this.friendID = friendId;
        this.senderName = senderName;
        this.message = message;
        this.type = type;
        this.messageTimeMillis = System.currentTimeMillis();
        this.fileOffer = fileMetaData;
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
    public String getSenderName() {
        return senderName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getMessageTimeMillis() {
        return messageTimeMillis;
    }

    @Override
    public FileMetaData getFileOffer() {
        return fileOffer;
    }

    @Override
    public boolean hasFileOffer() {
        return (fileOffer != null);
    }
}
