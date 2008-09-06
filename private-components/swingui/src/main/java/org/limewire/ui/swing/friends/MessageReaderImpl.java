package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.ChatState;

class MessageReaderImpl implements MessageReader {
    private final Friend friend;

    MessageReaderImpl(Friend friend) {
        this.friend = friend;
    }

    public void readMessage(final String message) {
        if (message != null) {
            final Message msg = newMessage(message, Message.Type.Received);
            new MessageReceivedEvent(msg).publish();
        }
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageImpl(friend.getName(), friend, message, type);
    }

    public void newChatState(ChatState chatState) {
        new ChatStateEvent(friend, chatState).publish();
    }
}
