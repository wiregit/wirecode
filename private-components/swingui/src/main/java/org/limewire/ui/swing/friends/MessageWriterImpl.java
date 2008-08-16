package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.concurrent.ThreadExecutor;

public class MessageWriterImpl implements MessageWriter {
    private final String localID;
    private final MessageWriter writer;

    public MessageWriterImpl(String localID, MessageWriter writer) {
        this.localID = localID;
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
        return new MessageImpl(localID, message, type);
    }
}
