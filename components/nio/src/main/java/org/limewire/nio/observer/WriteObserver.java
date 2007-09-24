package org.limewire.nio.observer;

/**
 * Defines the interface to allows write events to be received.
 * <p>
 * If the events are being received because of a <code>SelectableChannel</code>,
 * you can turn off interest in events via 
 * <code>NIODispatcher.instance().interestWrite(channel, false);</code>
 */
public interface WriteObserver extends IOErrorObserver {

    /**
     * Notification that a write can be performed.
     *
     * If there is still data to be written, this returns <code>true</code>.
     * Otherwise <code>handleWrite</code> returns <code>false</code>.
     */
    boolean handleWrite() throws java.io.IOException;
    
}