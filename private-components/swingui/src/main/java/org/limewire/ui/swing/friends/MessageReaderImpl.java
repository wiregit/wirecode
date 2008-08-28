package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.MessageReader;

class MessageReaderImpl implements MessageReader {
    private final Friend friend;

    MessageReaderImpl(Friend friend) {
        this.friend = friend;
    }

    public void readMessage(final String message) {
        final Message msg = newMessage(message, message == null ? Message.Type.Typing : Message.Type.Received);
        new MessageReceivedEvent(msg).publish();
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageImpl(friend.getName(), friend, message, type);
    }
}
