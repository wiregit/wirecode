package com.limegroup.gnutella.io;

interface ReadHandler extends NIOHandler {
    /**
     * Notification that a read can be performed.
     * If further reads are not wanted, this should unset the interest in reading
     * via:
     *   synchronized(channel.blockingLock()) {
     *      sk.interestOps(sk.interestOps() & ~SelectionKey.OP_READ);
     *   }
     *
     * This is necessary because implementations of handleRead may use multiple
     * threads that change the interest on this SelectionKey.  The only way to
     * ensure that interest is maintained correctly is to force reading
     * to set the interest itself.  The synchronization of blockingLock is necessary
     * because another thread may be attempting to change the interestOps concurrently,
     * which could cause one of the newer ops to be lost.
     */
    void handleRead(java.nio.channels.SelectionKey sk) throws java.io.IOException;
}