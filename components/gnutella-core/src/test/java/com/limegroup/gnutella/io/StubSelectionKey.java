/**
 * 
 */
package com.limegroup.gnutella.io;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

class StubSelectionKey extends SelectionKey {
    
    private volatile int interestOps;
    private int readyOps;
    private volatile boolean valid;
    private SelectableChannel channel;
    private Selector selector;
    
    StubSelectionKey(Selector selector, SelectableChannel channel, int ops, Object attachment) {
        this.selector = selector;
        this.channel = channel;
        this.interestOps = ops;
        attach(attachment);
    }
    
    void setReadyOps(int ops) {
        this.readyOps = ops;
    }

    public void cancel() {
        valid = false;
    }

    public SelectableChannel channel() {
        return channel;
    }

    public int interestOps() {
        return interestOps;
    }

    public SelectionKey interestOps(int ops) {
        if(!valid)
            throw new CancelledKeyException();
        this.interestOps = ops;
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public int readyOps() {
        if(!valid)
            throw new CancelledKeyException();
        return readyOps;
    }

    public Selector selector() {
        return selector;
    }
}