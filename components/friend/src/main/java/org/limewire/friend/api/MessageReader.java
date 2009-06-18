package org.limewire.friend.api;


/**
 * Called by the xmpp service when a chat message is received
 */
public interface MessageReader {
    public void readMessage(String message);

    public void newChatState(ChatState chatState);
}
