package com.limegroup.gnutella.io;

import java.nio.channels.*;

public class FakeSelectionKey extends SelectionKey {
    private int ops;
    
    public FakeSelectionKey(Object attch) {
        this(attch, true, true);
    }
    
    public FakeSelectionKey(Object attch, boolean r, boolean w) {
        super.attach(attch);
        ops |= SelectionKey.OP_READ;
        ops |= SelectionKey.OP_WRITE;
    }
    
    public void cancel() {}
    public SelectableChannel channel() { return null; }
    public int interestOps() { return ops; }
    public SelectionKey interestOps(int ops) { this.ops = ops; return this; }
    public boolean isValid() { return true; }
    public int readyOps() { return ops; }
    public Selector selector() { return null; }
}