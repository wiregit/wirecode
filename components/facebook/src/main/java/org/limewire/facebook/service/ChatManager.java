package org.limewire.facebook.service;

import java.util.Map;
import java.util.HashMap;

import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.client.ChatState;

/**
 * Manages message readers ({@link org.limewire.core.api.friend.client.MessageReader}) that ChatListener
 * uses to handle incoming instant messages and changing chat states.
 *
 * MessageReaders get added to ChatManager either by directly being
 * added via {@link #addMessageReader}, or by being created via
 * {@link IncomingChatListener#incomingChat(MessageWriter)}. Thus
 * IncomingChatListener objects are also managed by this class.
 *
 */
class ChatManager {

    private final Map<String, MessageReader> readers =
            new HashMap<String, MessageReader>();

    private final Map<String, IncomingChatListener> incomingChats =
            new HashMap<String, IncomingChatListener>();

    private final FacebookFriendConnection facebookFriendConnection;

    ChatManager(FacebookFriendConnection facebookFriendConnection) {
        this.facebookFriendConnection = facebookFriendConnection;
    }

    /**
     * Associates a friend ID with a MessageReader.  Happens when preparing to
     * initiate a new chat with a friend by sending a chat message.
     *
     * @param friendId ID of friend
     * @param reader {link @MessageReader} to associate it with
     * @return MessageWriter interface to use for sending chat messages
     */
    synchronized MessageWriter addMessageReader(String friendId, MessageReader reader) {
        readers.put(friendId, reader);
        return new MessageWriterImpl(friendId);
    }

    /**
     * Gets from existing collection of {@link MessageReader} objects
     * or creates one via the friend's {@link IncomingChatListener}.
     *
     * @param friendId ID of friend
     * @return MessageReader message reader associated with friend.
     */
    synchronized MessageReader getMessageReader(String friendId) {
        MessageReader handler = readers.get(friendId);

        if (handler == null) {
            handler = processNewIncomingChat(friendId);
        }
        return handler;
    }

    /**
     * Sets an incoming chat listener for the friend whose ID id friendId
     * A MessageReader will get associated with the friendId upon a chat being
     * initiated by the friend.
     *
     */
    synchronized void setIncomingChatListener(String friendId, IncomingChatListener listener) {
        incomingChats.put(friendId, listener);
    }

    /**
     * Stops managing chats for friendId.
     *
     * @param friendId ID of friend
     */
    synchronized void removeChat(String friendId) {
        incomingChats.remove(friendId);
        readers.remove(friendId);
    }

    private MessageReader processNewIncomingChat(String friendId) {
        IncomingChatListener listener = incomingChats.get(friendId);

        if (listener != null) {
            MessageWriter writer = new MessageWriterImpl(friendId);
            MessageReader reader = listener.incomingChat(writer);
            readers.put(friendId, reader);
            return reader;
        }
        return null;
    }

    private class MessageWriterImpl implements MessageWriter {

        private final String friendId;

        MessageWriterImpl(String friendId) {
            this.friendId = friendId;
        }

        @Override
        public void writeMessage(String message) throws FriendException {
            facebookFriendConnection.sendChatMessage(friendId, message);
        }

        @Override
        public void setChatState(ChatState chatState) throws FriendException {
            facebookFriendConnection.sendChatStateUpdate(friendId, chatState);
        }
    }
}
