package com.limegroup.gnutella.io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.util.ThreadFactory;

/**
 * A socket that allows asynchronous connections but is backed
 * by a legacy I/O Socket.
 * 
 * This is intended primarily for ease-of-use debugging in swapping out
 * an NIOSocket with a BlockingSocketAdapter, so that NIO code can be
 * compared to regular I/O code.
 */
public class BlockingSocketAdapter extends NBSocket {

    public BlockingSocketAdapter() {
        super();
    }
    
    public BlockingSocketAdapter(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }
    
    public BlockingSocketAdapter(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }
    
    public BlockingSocketAdapter(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }
    
    public BlockingSocketAdapter(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }

    /**
     * This will spawn a new thread and perform the blocking connect on that thread.
     * The observer will be notified of the changes.
     */
    public boolean connect(final SocketAddress addr, final int timeout, 
                           final ConnectObserver observer) throws IOException {
        ThreadFactory.startThread(new Runnable() {
            public void run() {
                try {
                    connect(addr, timeout);
                    observer.handleConnect(BlockingSocketAdapter.this);
                } catch(IOException x) {
                    observer.shutdown();
                }
            }
        }, "BlockingSocketEmulator");
        return false;
    }
}
