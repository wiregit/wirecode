package com.limegroup.gnutella.io;

public interface ReadHandler extends NIOHandler {
    /**
     * Notification that a read can be performed.
     * If further reads are not wanted, this should unset the interest in reading via:
     *   NIODispatcher.instance().interestRead(channel, false);
     */
    void handleRead() throws java.io.IOException;
}