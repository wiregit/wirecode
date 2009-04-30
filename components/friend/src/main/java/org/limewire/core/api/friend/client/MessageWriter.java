package org.limewire.core.api.friend.client;

import org.limewire.xmpp.api.client.XMPPException;

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

    /**
     * If necessary, sends a message indicating the new
     * chat state
     *
     * @param chatState
     * @throws XMPPException
     */
    public void setChatState(ChatState chatState) throws XMPPException;
}
