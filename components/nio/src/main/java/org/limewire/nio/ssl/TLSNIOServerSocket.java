package org.limewire.nio.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.limewire.nio.NIOServerSocket;
import org.limewire.nio.observer.AcceptObserver;

/**
 * A server socket that listens for incoming TLS-capable connections.
 * 
 * This is currently hardcoded to use TLSNIOSocket, which supports only:
 *  - TLS_DH_anon_WITH_AES_128_CBC_SHA
 */
public class TLSNIOServerSocket extends NIOServerSocket {

    public TLSNIOServerSocket(AcceptObserver observer) throws IOException {
        super(observer);
    }

    public TLSNIOServerSocket(int port, AcceptObserver observer) throws IOException {
        super(port, observer);
    }

    public TLSNIOServerSocket(int port, int backlog, AcceptObserver observer) throws IOException {
        super(port, backlog, observer);
    }

    public TLSNIOServerSocket(int port, int backlog, InetAddress bindAddr, AcceptObserver observer) throws IOException {
        super(port, backlog, bindAddr, observer);
    }

    public TLSNIOServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        super(port, backlog, bindAddr);
    }

    public TLSNIOServerSocket(int port, int backlog) throws IOException {
        super(port, backlog);
    }

    public TLSNIOServerSocket(int port) throws IOException {
        super(port);
    }

    public TLSNIOServerSocket() throws IOException {
        super();
    }

    @Override
    protected Socket createClientSocket(Socket socket) {
        return new TLSNIOSocket(socket);
    }
    
    

}
