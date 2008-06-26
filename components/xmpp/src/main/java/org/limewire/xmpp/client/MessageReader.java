package org.limewire.xmpp.client;

/**
 * Called by the xmpp service when a chat message is received
 */
public interface MessageReader {
    void readMessage(String message);
}
