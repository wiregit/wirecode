package com.limegroup.gnutella.io;

import java.nio.channels.SocketChannel;
import java.io.IOException;

/**
 * Allows accept events to be received.
 *
 * If the events are being received because of a SelectableChannel,
 * interest in events can be turned off by using:
 *  NIODispatcher.instance().interestAccept(channel, false);
 */
interface AcceptObserver extends IOErrorObserver {
    
    /**
     *  Notification that a SocketChannel has been accepted.
     *  The channel is in non-blocking mode.
     */
    void handleAccept(SocketChannel channel) throws IOException;
}