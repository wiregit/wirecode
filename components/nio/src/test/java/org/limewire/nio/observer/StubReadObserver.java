package org.limewire.nio.observer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.limewire.nio.observer.ReadObserver;
import org.limewire.nio.timeout.ReadTimeout;

public class StubReadObserver implements ReadObserver, ReadTimeout {
    
    private volatile long readTimeout = 0;
    private volatile boolean shutdown = false;
    private volatile IOException iox;
    private volatile SocketChannel channel;
    private volatile int readsHandled;
    private volatile ByteBuffer buffer;
    private volatile boolean ignoreReadData = false;
    
    public ByteBuffer getReadBuffer() {
        return buffer;
    }
    
    public void setIgnoreReadData(boolean ignore) {
        ignoreReadData = ignore;
    }
    
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
        if(!ignoreReadData) {
            if(buffer == null)
                buffer = ByteBuffer.allocate(1024);
            while(buffer.hasRemaining() && channel.read(buffer) != 0);
        }
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
