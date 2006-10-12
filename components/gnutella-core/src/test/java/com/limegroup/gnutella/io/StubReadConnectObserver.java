package com.limegroup.gnutella.io;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class StubReadConnectObserver implements ReadObserver, ConnectObserver, ReadTimeout {
    private StubConnectObserver connectDelegate = new StubConnectObserver();
    private StubReadObserver readDelegate = new StubReadObserver();
    private IOException ioException;
    private boolean shutdown;
    private SocketChannel channel;
    private long ioxTime;
    private long lastReadTime;
    
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
