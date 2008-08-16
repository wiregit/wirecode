package org.limewire.ui.swing.friends;

import javax.swing.SwingUtilities;

import org.limewire.xmpp.api.client.MessageReader;

class MessageReaderImpl implements MessageReader {
    private final Friend friend;

    MessageReaderImpl(Friend friend) {
        this.friend = friend;
    }
    
    public void readMessage(final String message) {
        final Message msg = newMessage(message, Message.Type.Received);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MessageReceivedEvent(msg).publish();
                //handleMessage(msg);
            }
        });
        //new MessageReceivedEvent(msg).publish();
    }
    
    private Message newMessage(String message, Message.Type type) {
        return new MessageImpl(friend.getName(), message, type);
    }
}