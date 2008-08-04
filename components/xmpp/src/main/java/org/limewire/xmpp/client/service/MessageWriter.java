package org.limewire.xmpp.client.service;

import org.limewire.xmpp.client.impl.XMPPException;

/**
 * Called by the user of the xmpp container to send a chat message
 */
public interface MessageWriter {
    /**
     * Sends a message to the <code>Presence</code>; blocking call.
     * @param message
     * @throws XMPPException
     */
    public void writeMessage(String message) throws XMPPException;    
}
