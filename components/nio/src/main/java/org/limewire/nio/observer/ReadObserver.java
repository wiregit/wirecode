package org.limewire.nio.observer;

import java.io.IOException;

/**
 * Defines the interface that allows read events to be received.
 * <p>
 * If the events are being received because of a <code>SelectableChannel</code>,
 * you can turn off interest in events via
 * <code>NIODispatcher.instance().interestRead(channel, false)</code>.
 */
public interface ReadObserver extends IOErrorObserver {
    
    /** Notification that a read can be performed. */
    void handleRead() throws IOException;    
}
    
    