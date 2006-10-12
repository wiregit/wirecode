package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class StubReadObserver implements ReadObserver, ReadTimeout {
    
    private long readTimeout = 0;
    private boolean shutdown = false;
    private IOException iox;
    private SocketChannel channel;
    private volatile int readsHandled;
    private ByteBuffer buffer;
    
    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel getChannel() throws IOException {
        if(channel == null) {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
        }
        return channel;
    }

    public IOException getIox() {
        return iox;
    }

    public int getReadsHandled() {
        return readsHandled;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void handleRead() throws IOException {
        readsHandled++;
        if(buffer == null)
            buffer = ByteBuffer.allocate(1024);
        while(buffer.hasRemaining() && channel.read(buffer) != 0);
    }

    public synchronized void handleIOException(IOException iox) {
        this.iox = iox;
        notify();
    }

    public void shutdown() {
        shutdown = true;
    }

    public long getReadTimeout() {
        return readTimeout;
    }
    
    public synchronized void waitForIOException(long waitTime) throws InterruptedException {
        wait(waitTime);
    }

}
