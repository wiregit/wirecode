package org.limewire.ui.swing.friends;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;

class MessageWriterImpl implements MessageWriter {
    private final String localID;
    private final Friend friend;
    private final MessageWriter writer;

    MessageWriterImpl(String localID, Friend friend, MessageWriter writer) {
        this.localID = localID;
        this.friend = friend;
        this.writer = writer;
    }

    public void writeMessage(final String message) throws XMPPException {
        ThreadExecutor.startThread(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.writeMessage(message);
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        }, "send-message");
        new MessageReceivedEvent(newMessage(message, Message.Type.Sent)).publish();
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageImpl(localID, friend, message, type);
    }

    public void setChatState(ChatState chatState) throws XMPPException {
        writer.setChatState(chatState);
    }
}
