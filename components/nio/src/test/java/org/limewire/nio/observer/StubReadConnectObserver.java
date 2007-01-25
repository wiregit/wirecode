package org.limewire.nio.observer;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.ReadObserver;
import org.limewire.nio.timeout.ReadTimeout;

public class StubReadConnectObserver implements ReadObserver, ConnectObserver, ReadTimeout {
    private volatile StubConnectObserver connectDelegate = new StubConnectObserver();
    private volatile StubReadObserver readDelegate = new StubReadObserver();
    private volatile IOException ioException;
    private volatile boolean shutdown;
    private volatile SocketChannel channel;
    private volatile long ioxTime;
    private volatile long lastReadTime;
    
    public IOException getIoException() {
        return ioException;
    }
    
    public boolean isShutdown() {
        return shutdown;
    }
    
    public synchronized void shutdown() {
        this.shutdown = true;
        notify();
    }
    
    public void setChannel(SocketChannel channel) {
        this.channel = channel;
        readDelegate.setChannel(channel);
    }
    
    public SocketChannel getChannel() throws IOException {
        if(channel == null) {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            readDelegate.setChannel(channel);
        }
        return channel;
    }
    
    public synchronized void handleIOException(IOException iox) {
        this.ioxTime = System.currentTimeMillis();
        this.ioException = iox;
        notify();
    }
    
    public Socket getSocket() {
        return connectDelegate.getSocket();
    }
    
    public synchronized void handleConnect(Socket socket) throws IOException {
        connectDelegate.handleConnect(socket);
        notify();
    }
    
    public synchronized int getReadsHandled() {
        return readDelegate.getReadsHandled();
    }
    
    public long getReadTimeout() {
        return readDelegate.getReadTimeout();
    }
    
    public synchronized void handleRead() throws IOException {
        this.lastReadTime = System.currentTimeMillis();
        readDelegate.handleRead();
        notify();
    }
    
    public void setReadTimeout(long readTimeout) {
        readDelegate.setReadTimeout(readTimeout);
    }
    
    public synchronized void waitForEvent(long timeout) throws InterruptedException {
        wait(timeout);
    }

    public long getIoxTime() {
        return ioxTime;
    }

    public void setIoxTime(long ioxTime) {
        this.ioxTime = ioxTime;
    }

    public long getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(long lastReadTime) {
        this.lastReadTime = lastReadTime;
    }
}
