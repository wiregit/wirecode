/**
 * 
 */
package com.limegroup.gnutella.stubs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class StubSocket extends Socket {

    public InetAddress address;

    public int port;

    public boolean closed;

    public StubSocket(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public StubSocket() {
    }

    @Override
    public synchronized void close() throws IOException {
        this.closed = true;
    }

    @Override
    public InetAddress getInetAddress() {
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }
}