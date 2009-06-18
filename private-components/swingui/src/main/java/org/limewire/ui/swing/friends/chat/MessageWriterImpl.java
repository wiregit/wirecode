package org.limewire.ui.swing.friends.chat;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.MessageWriter;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

class MessageWriterImpl implements MessageWriter {
    private static final Log LOG = LogFactory.getLog(MessageWriterImpl.class);
    
    private final String localID;
    private final ChatFriend chatFriend;
    private final MessageWriter writer;

    MessageWriterImpl(String localID, ChatFriend chatFriend, MessageWriter writer) {
        this.localID = localID;
        this.chatFriend = chatFriend;
        this.writer = writer;
    }

    @Override
    public void writeMessage(final String message) throws FriendException {
        ThreadExecutor.startThread(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.writeMessage(message);
                } catch (FriendException e) {
                    LOG.error("send message failed", e);
                }
            }
        }, "send-message");
        new MessageReceivedEvent(newMessage(message, Message.Type.Sent)).publish();
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageTextImpl(localID, chatFriend.getID(), type, message);
    }

    @Override
    public void setChatState(final ChatState chatState) throws FriendException {
        ThreadExecutor.startThread(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.setChatState(chatState);
                } catch (FriendException e) {
                    LOG.error("set chat state failed", e);
                }
            }
        }, "set-chat-state");
    }
}
