package com.limegroup.gnutella.connection;

import org.limewire.nio.ProtocolBandwidthTracker;

public class SimpleProtocolBandwidthTracker implements ProtocolBandwidthTracker {
    
    private volatile long read;
    private volatile long written;
    
    public void addRead(int read) {
        this.read += read;
    }
    
    public void addWritten(int written) {
        this.written += written;
    }

    public long getReadBytesConsumed() {
        return read;
    }

    public long getReadBytesProduced() {
        return read;
    }

    public long getWrittenBytesConsumed() {
        return written;
    }

    public long getWrittenBytesProduced() {
        return written;
    }

}
