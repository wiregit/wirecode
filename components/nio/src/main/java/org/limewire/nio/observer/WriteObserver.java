package org.limewire.nio.observer;

/**
 * Allows write events to be received.
 *
 * If the events are being received because of a SelectableChannel,
 * interest in events can be turned off by using:
 *  NIODispatcher.instance().interestWrite(channel, false);
 */
public interface WriteObserver extends IOErrorObserver {

    /**
     * Notification that a write can be performed.
     *
     * If there is still data to be written, this returns true.
     * Otherwise this returns false.
     */
    boolean handleWrite() throws java.io.IOException;
    
}