package org.limewire.nio.observer;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Allows accept events to be received.
 *
 * If the events are being received because of a SelectableChannel,
 * interest in events can be turned off by using:
 *  NIODispatcher.instance().interestAccept(channel, false);
 */
public interface AcceptChannelObserver extends IOErrorObserver {
    
    /**
     *  Notification that a SocketChannel has been accepted.
     *  The channel is in non-blocking mode.
     */
    void handleAcceptChannel(SocketChannel channel) throws IOException;
}