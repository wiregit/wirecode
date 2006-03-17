package com.limegroup.gnutella.udpconnect;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

class UDPSelectionKey extends SelectionKey {
    
    private UDPConnectionProcessor processor;
    private volatile boolean valid = true;
    
    private int interestOps = 0;
    
    UDPSelectionKey(UDPConnectionProcessor processor) {
        this.processor = processor;
    }
    
    UDPSelectionKey(UDPConnectionProcessor processor, Object attachment) {
        this.processor = processor;
        attach(attachment);
    }

    public void cancel() {
        valid = false;
    }

    public SelectableChannel channel() {
        throw new UnsupportedOperationException();
    }

    public int interestOps() {
        return interestOps;
    }

    public SelectionKey interestOps(int ops) {
        this.interestOps = ops;
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public int readyOps() {
        return processor.isReadReady() ? SelectionKey.OP_READ : 0;
    }

    public Selector selector() {
        throw new UnsupportedOperationException();
    }
}
