
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.nio.channels.SocketChannel;
import java.io.IOException;

/**
 * NIO can tell you a remote computer has connected, handleAccept().
 * 
 * An object implements AcceptObserver so NIO can call its handleAccept() method when a new remote computer connects to it.
 * 
 * NIO calls handleAccept() because of a SelectableChannel that's listening for incoming connections. (do)
 * Use this line of code to get NIO to stop calling handleAccept():
 * NIODispatcher.instance().interestAccept(channel, false);
 * 
 * Only one class in LimeWire implements this interface: NIOServerSocket.
 * NIO calls NIOServerSocket.handleAccept() when a new computer has connected to us.
 */
interface AcceptObserver extends IOErrorObserver {

    /**
     * NIO calls handleAccept(channel) when a new remote computer has connected to us.
     * The given channel is our new connection to the remote computer.
     * The channel starts out in non-blocking mode, which is what we want.
     * 
     * @param channel A new SocketChannel object from NIO that we can use to exchange data with the remote compuer that just connected to us.
     */
    void handleAccept(SocketChannel channel) throws IOException;
}
