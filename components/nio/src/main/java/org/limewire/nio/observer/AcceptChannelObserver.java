package org.limewire.nio.observer;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Defines the interface that allows <code>SocketChannel</code> accept events 
 * to be received.
 * <p>
 * If the events are being received because of a <code>SelectableChannel</code>,
 * you can turn off interest in events via
 * <code>NIODispatcher.instance().interestAccept(channel, false)</code>.
 * 
 */
public interface AcceptChannelObserver extends IOErrorObserver {
    
    /**
     *  Notification that a SocketChannel has been accepted.
     *  The channel is in non-blocking mode.
     */
    void handleAcceptChannel(SocketChannel channel) throws IOException;
}