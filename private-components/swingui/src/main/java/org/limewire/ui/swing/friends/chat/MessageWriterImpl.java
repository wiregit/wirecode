package org.limewire.ui.swing.friends.chat;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.MessageWriter;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.util.I18n;

class MessageWriterImpl implements MessageWriter {
    private static final Log LOG = LogFactory.getLog(MessageWriterImpl.class);
    
    private final ChatFriend chatFriend;
    private final MessageWriter writer;

    MessageWriterImpl(ChatFriend chatFriend, MessageWriter writer) {
        this.chatFriend = chatFriend;
        this.writer = writer;
    }

    @Override
    public void writeMessage(final String message) throws FriendException {
        
        if (chatFriend.isSignedIn()) {
            ThreadExecutor.startThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        writer.writeMessage(message);
                    } catch (FriendException e) {
                        // todo: have a way of reporting the exception!
                        LOG.error("send message failed", e);
                    }
                }
            }, "send-message");
            new MessageReceivedEvent(
                newMessage(message, Message.Type.SENT)).publish();
        } else {
            new MessageReceivedEvent(
                newMessage(message, Message.Type.SEND_FAILED_SIGNOUT)).publish();
        }
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageTextImpl(I18n.tr("me"), chatFriend.getID(), type, message);
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
