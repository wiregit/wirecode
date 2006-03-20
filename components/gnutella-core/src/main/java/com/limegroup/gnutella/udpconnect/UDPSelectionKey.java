package com.limegroup.gnutella.udpconnect;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * A selection key for UDP connections.
 */
class UDPSelectionKey extends SelectionKey {
    
    private final UDPConnectionProcessor processor;
    private final SelectableChannel channel;
    private volatile boolean valid = true;
    
    private int interestOps = 0;
    
    UDPSelectionKey(UDPConnectionProcessor processor, Object attachment,
                    SelectableChannel channel, int ops) {
        this.processor = processor;
        this.channel = channel;
        interestOps(ops);
        attach(attachment);
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
        this.interestOps = ops;
        return this;
    }

    public boolean isValid() {
        return valid;
    }

    public int readyOps() {
        synchronized(processor) {
            return (processor.isReadReady()    ? SelectionKey.OP_READ  : 0)
                 | (processor.isWriteReady()   ? SelectionKey.OP_WRITE : 0)
                 | (processor.isConnectReady() ? SelectionKey.OP_CONNECT : 0);
        }
    }

    public Selector selector() {
        throw new UnsupportedOperationException();
    }
}
