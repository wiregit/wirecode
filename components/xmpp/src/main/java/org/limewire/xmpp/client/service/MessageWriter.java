package org.limewire.xmpp.client.service;

import org.limewire.xmpp.client.impl.XMPPException;

/**
 * Called by the user of the xmpp container to send a chat message
 */
public interface MessageWriter {
    public void writeMessage(String message) throws XMPPException;    
}
