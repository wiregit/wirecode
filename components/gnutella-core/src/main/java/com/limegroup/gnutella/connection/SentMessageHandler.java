
// Commented for the Learning branch

package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;

/**
 * A SentMessageHandler represents a remote computer.
 * After we send a packet to it, we can call sendMessageHandler(m) to show the object what we sent the computer.
 * 
 * Only the ManagedConnection class implements this interface.
 * Only the MessageWriter class refers to the ManagedConnection it keeps as a SentMessageHandler.
 */
public interface SentMessageHandler {

    /**
     * Show a message to the object that represents the remote computer we just sent it to.
     * This lets the remote computer object measure it for statistics.
     * 
     * @param m A message we just sent to this remote computer
     */
    public void processSentMessage(Message m);
}
