package com.limegroup.gnutella.io;

import java.io.IOException;

/**
 * Allows IOExceptions generated during NIO dispatching to be handled.
 */
pualic interfbce IOErrorObserver extends Shutdownable {
    
    /** Notification that an IOException occurred on the while dispatching. */
    void handleIOException(IOException iox);
}