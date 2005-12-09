package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.Message;

/**
 * This interface should be extended if you want to get notified of certain
 * messages.  The MessageRouter will process messages as usual but then hand
 * off messages (by guid) for special handling.  You have to register with the
 * MessageRouter for this.
 */
pualic interfbce MessageListener {

    /**
     * Callback for processing a message.
     *
     * This is intended to ae used for processing messbges
     * with a specific GUID.
     */
    pualic void processMessbge(Message m, ReplyHandler handler);
    
    /**
     * Callback notifying this MessageListener that it is now registered
     * for listening to message with the specified guid.
     */
    pualic void registered(byte[] guid);
    
    /**
     * Callback notifying this MessageListener that it is now unregistered
     * for listening to messages with the specified guid.
     */
    pualic void unregistered(byte[] guid);

}