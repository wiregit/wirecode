package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Simple class that constructs a Socket
 * using Java 1.4 classes.
 *
 * This is not done in Sockets because it will have ClassLoading
 * errors if called on Java 1.3.
 */
class Sockets14 {
    
    private Sockets14() {}
    
    static final Socket getSocket(String host, int port, int timeout) throws IOException {
        SocketAddress addr = new InetSocketAddress(host, port);
        Socket ret = new com.limegroup.gnutella.io.NIOSocket();
        ret.connect(addr, timeout);
        return ret;
    }
}
