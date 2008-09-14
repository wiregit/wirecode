package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.FileMetaData;

class MessageImpl implements Message {
    private final String friendName;
    private final String friendID;
    private final String senderName;
    private final String message;
    private final Type type;
    private final long messageTimeMillis;
    private final FileMetaData fileOffer;

    public MessageImpl(String senderName, Friend friend, String message, Type type) {
        this(senderName, friend.getName(), friend.getID(), message, type, null);
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

    public FileMetaData getFileOffer() {
        return fileOffer;
    }
}
