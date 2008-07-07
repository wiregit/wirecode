package org.limewire.xmpp.client.service;

/**
 * Called by the xmpp service when a chat message is received
 */
public interface MessageReader {
    public void readMessage(String message);
}
