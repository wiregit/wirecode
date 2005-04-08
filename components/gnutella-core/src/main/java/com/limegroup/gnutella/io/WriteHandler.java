package com.limegroup.gnutella.io;

public interface WriteHandler extends NIOHandler {
    /**
     * Notification that a write can be performed.
     * If further writes are not wanted, this should unset the interest in writing via:
     *   NIODispatcher.instance().interestWrite(channel, false);
     */
    void handleWrite() throws java.io.IOException;
    
}