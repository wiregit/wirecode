package com.limegroup.gnutella.io;

interface WriteHandler extends NIOHandler {
    /**
     * Notification that a write can be performed.
     * If further writes are not wanted, this should unset the interest in writing
     * via:
     *   sk.interestOps(sk.interestOps() & ~SelectionKey.OP_WRITE);
     *
     * This is necessary because implementations of handleWrite may use multiple
     * threads that change the interest on this SelectionKey.  The only way to
     * ensure that interest is maintained correctly is to force writing
     * to set the interest itself.
     */
    void handleWrite(java.nio.channels.SelectionKey sk) throws java.io.IOException;
    
}