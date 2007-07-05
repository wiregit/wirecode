/**
 * 
 */
package org.limewire.http;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class StubChannel extends SocketChannel {
    private boolean connect;
    private SocketAddress connectAddr;
    private boolean finishConnect;
    private boolean connected;
    private boolean connectionPending;
    private Socket socket;
    
    private volatile int readyOps;
    
    
    StubChannel() {
        super(null);
        try {
            configureBlocking(false);
        } catch(IOException err) {
            throw new RuntimeException(err);
        }
    }

    public boolean connect(SocketAddress remote) throws IOException {
        this.connectAddr = remote;
        return connect;
    }

    public boolean finishConnect() throws IOException {
        this.finishConnect = true;
        return true;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isConnectionPending() {
        return connectionPending;
    }

    public int read(ByteBuffer dst) throws IOException {
        return 0;
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return 0;
    }

    public Socket socket() {
        return socket;
    }
    
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public int write(ByteBuffer src) throws IOException {
        return 0;
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return 0;
    }

    protected void implCloseSelectableChannel() throws IOException {
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
    }

    public SocketAddress getConnectAddr() {
        return connectAddr;
    }

    public boolean isFinishConnect() {
        return finishConnect;
    }

    public void setConnect(boolean connect) {
        this.connect = connect;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setConnectionPending(boolean connectionPending) {
        this.connectionPending = connectionPending;
    }

    public int readyOps() {
        return readyOps;
    }

    public void setReadyOps(int readyOps) {
        this.readyOps = readyOps;
    }
    
}
