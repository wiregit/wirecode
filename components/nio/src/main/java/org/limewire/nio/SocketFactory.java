package org.limewire.nio;

import java.io.IOException;
import java.net.ServerSocket;

import org.limewire.nio.observer.AcceptObserver;

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
    
    /**
     * Returns a new ServerSocket with the observer.
     * The socket is NOT BOUND.
     */
    public static ServerSocket newServerSocket(AcceptObserver observer) throws IOException {
        return new NIOServerSocket(observer); // based on NIO
       //  return new BlockingServerSocketAdapter(observer); // based on IO
    }
    
}
