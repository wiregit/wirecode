package com.limegroup.gnutella.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import com.limegroup.gnutella.Assert;

/**
 * An OutputStream that delegates to a Channel.  Designed to work-around apparent
 * bugs in the default channel output streams provided by Java 1.4.0/1.4.1.  Does
 * not buffer; writes one byte at a time.  This is inefficient but allows mixing
 * of blocking and non-blocking IO.
 */
public class ChannelOutputStream extends OutputStream {
    /** The single buffered byte.  Between writes, this' position must be zero
     *  and capacity 1. */
    ByteBuffer _buf=ByteBuffer.allocate(1);
    SocketChannel _channel;

    /**
     * Creates a new output stream that delegates to channel.  No other thread
     * may write to channel while this is in use.  Configures channel for
     * blocking IO.
     * @param channel the delegate for IO operations
     * @exception IOException couldn't make channel non-blocking
     */
    public ChannelOutputStream(SocketChannel channel) throws IOException {
        this._channel=channel;
        _channel.configureBlocking(true);
    }

    public void write(int b) throws IOException {
        repOk();
        _buf.put(0, (byte)b);
        try {
            int n=_channel.write(_buf);
            Assert.that(n==1, "Wrote unexpected number of bytes");
        } finally {
            _buf.clear();
            repOk();
        }
    }

    public void close() throws IOException {
        _channel.close();
    }

    protected void repOk() {
        Assert.that(_buf.position()==0, "Bad position");
        Assert.that(_buf.limit()==1, "Bad limit");      
        Assert.that(_buf.capacity()==1, "Bad capacity");  
    }

    //Unit test: tests/com/limegroup/gnutella/util/ChannelStreamsTest.java
}
