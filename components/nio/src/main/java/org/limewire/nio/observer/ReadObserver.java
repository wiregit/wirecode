package org.limewire.nio.observer;

import java.io.IOException;

/**
 * Allows read events to be received.
 *
 * If the events are being received because of a SelectableChannel,
 * interest in events can be turned off by using:
 *  NIODispatcher.instance().interestRead(channel, false);
 */
public interface ReadObserver extends IOErrorObserver {
    
    /** Notification that a read can be performed */
    void handleRead() throws IOException;    
}
    
    