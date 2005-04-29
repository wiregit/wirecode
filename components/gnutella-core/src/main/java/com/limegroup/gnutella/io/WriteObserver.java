package com.limegroup.gnutella.io;

/**
 * Allows write events to be received.
 *
 * If the events are being received because of a SelectableChannel,
 * interest in events can be turned off by using:
 *  NIODispatcher.instance().interestWrite(channel, false);
 */
public interface WriteObserver extends IOErrorObserver {

    /** Notification that a write can be performed. */
    void handleWrite() throws java.io.IOException;
    
}