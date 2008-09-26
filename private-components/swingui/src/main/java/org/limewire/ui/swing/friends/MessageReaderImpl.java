package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.ChatState;

class MessageReaderImpl implements MessageReader {
    private final ChatFriend chatFriend;

    MessageReaderImpl(ChatFriend chatFriend) {
        this.chatFriend = chatFriend;
    }

    public void readMessage(final String message) {
        if (message != null) {
            final Message msg = newMessage(message, Message.Type.Received);
            new MessageReceivedEvent(msg).publish();
        }
    }

    private Message newMessage(String message, Message.Type type) {
        return new MessageImpl(chatFriend.getName(), chatFriend, message, type);
    }

    public void newChatState(ChatState chatState) {
        new ChatStateEvent(chatFriend, chatState).publish();
    }
}
