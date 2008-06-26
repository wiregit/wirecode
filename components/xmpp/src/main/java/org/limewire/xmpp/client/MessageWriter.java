package org.limewire.xmpp.client;

/**
 * Called by the user of the xmpp container to send a chat message
 */
public interface MessageWriter {
    void writeMessage(String message) throws XMPPException;    
}
