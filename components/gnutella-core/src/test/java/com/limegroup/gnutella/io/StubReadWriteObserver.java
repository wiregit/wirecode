package com.limegroup.gnutella.io;

import java.io.IOException;

class StubReadWriteObserver implements ReadWriteObserver {
    
    private int amtToUse = Integer.MAX_VALUE;
    private Throttle throttle;
    private int read = 0;
    private int wrote = 0;
    private boolean shutdown = false;
    private IOException iox = null;
    private int amountGiven = 0;
    
    void setThrottle(Throttle throttle) {
        this.throttle = throttle;
    }
    
    public void handleRead() throws IOException {
        read++;
        amountGiven = throttle.request();
        throttle.release(Math.max(0, amountGiven - amtToUse));
    }
    
    public boolean handleWrite() throws IOException {
        wrote++;
        amountGiven = throttle.request();
        throttle.release(Math.max(0, amountGiven - amtToUse));
        return false;
    }
    
    public void handleIOException(IOException iox) {
        this.iox = iox;
    }
    
    public void shutdown() {
        shutdown = true;
    }
    
    int read() { return read; }
    int wrote() { return wrote; }
    int given() { return amountGiven; }
    IOException iox() { return iox; }
    boolean closed() { return shutdown; }
    
}