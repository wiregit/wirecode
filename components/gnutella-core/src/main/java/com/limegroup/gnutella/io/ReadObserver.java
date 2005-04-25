package com.limegroup.gnutella.io;

import java.io.IOException;

/**
 * Observer interface for objects wishing to receive notification
 * when a read is allowed.
 */
public interface ReadObserver {
    
    /** Notification that a read can be performed */
    void handleRead() throws IOException;
    
    /** Informs the observer that it can release any internal resources. */
    void shutdown();
    
}
    
    