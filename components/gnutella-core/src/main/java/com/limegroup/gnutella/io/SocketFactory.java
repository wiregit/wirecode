package com.limegroup.gnutella.io;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * A factory that will return new ServerSockets or NBSockets.
 */
public class SocketFactory {

    /**
     * Returns an unconnected NBSocket.
     */
    public static NBSocket newSocket() throws IOException {
        return new NIOSocket(); // based on NIO
      //   return new BlockingSocketAdapter(); // based on IO
    }
    
    /**
     * Returns a new ServerSocket.
     */
    public static ServerSocket newServerSocket(int port, AcceptObserver observer) throws IOException {
        return new NIOServerSocket(port, observer); // based on NIO
       //  return new BlockingServerSocketAdapter(port, observer); // based on IO
    }
    
}
