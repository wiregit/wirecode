package org.limewire.nio.observer;

import java.io.IOException;

/**
 * Allows IOExceptions generated during NIO dispatching to be handled.
 */
public interface IOErrorObserver extends Shutdownable {
    
    /** Notification that an IOException occurred on the while dispatching. */
    void handleIOException(IOException iox);
}