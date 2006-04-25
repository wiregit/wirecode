package com.limegroup.gnutella.io;

import java.io.IOException;

class StubReadWriteObserver implements IOErrorObserver, ReadWriteObserver {
    
    private int amtToUse = Integer.MAX_VALUE;
    private Throttle throttle;
    private int read = 0;
    private int wrote = 0;
    private boolean shutdown = false;
    private IOException iox = null;
    private int amountGiven = 0;
    private int amountLeft = 0;
    
    void setAmountGiven(int given) {
        amountGiven = given;
    }
    
    int getAmountLeft() {
        return amountLeft;
    }
    
    public void handleRead() throws IOException {
        read++;
        amountLeft = Math.max(0, amountGiven - amtToUse);
    }
    
    public boolean handleWrite() throws IOException {
        wrote++;
        amountLeft = Math.max(0, amountGiven - amtToUse);
        return false;
    }
    
    public void handleIOException(IOException iox) {
        this.iox = iox;
    }
    
    public void shutdown() {
        shutdown = true;
    }
    
    void setAmountToUse(int toUse) { amtToUse = toUse; }
    int read() { return read; }
    int wrote() { return wrote; }
    IOException iox() { return iox; }
    boolean closed() { return shutdown; }
    
}