package org.limewire.xmpp.client;

public interface IncomingChatListener {
    public MessageReader incomingChat(MessageWriter writer);
}
