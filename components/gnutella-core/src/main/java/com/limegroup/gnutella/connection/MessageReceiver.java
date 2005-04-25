package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;

/**
 * Notifications & information about asynchronous message processing.
 */
public interface MessageReceiver {
    
    /**
     * Notification that a message is available for processing.
     */
    public void processMessage(Message m);
    
    /**
     * The soft-max this message-receiver uses for creating messages.
     */
    public byte getSoftMax();
    
    /**
     * The network this message-receiver uses for creating messages.
     */
    public int getNetwork();
    
    /**
     * Notification that the stream is closed.
     */
    public void messagingClosed();
}
        