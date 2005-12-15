
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;

/**
 * NIO can tell you your connection is made, handleConnect().
 * 
 * An object implements ConnectObserver so NIO can call its handleConnect() method when the connection it initiated goes through.
 * 
 * NIO calls handleConnect() because of a SelectableChannel that's trying to open a new connection.
 * Use this line of code to get NIO to not call handleConnect():
 * NIODispatcher.instance().interestConnect(channel, false);
 * 
 * Only one class in LimeWire implements this interface: NIOSocket.
 * NIO calls NIOSocket.handleConnect() when the connection is made.
 */
interface ConnectObserver extends IOErrorObserver {

    /**
     * NIO calls handleConnect() when the connection we initiated is made.
     * 
     * Make a new NIOSocket object, and call connect() on it to try to open a new connection to a given IP address and port number.
     * When the connection is made, NIO will call the NIOSocket's handleConnect() method.
     */
    void handleConnect() throws IOException;
}
