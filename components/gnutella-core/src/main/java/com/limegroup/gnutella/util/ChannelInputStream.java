package com.limegroup.gnutella.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import com.limegroup.gnutella.Assert;

/**
 * An InputStream that delegates to a Channel.  Designed to work-around apparent
 * bugs in the default channel input streams provided by Java 1.4.0/1.4.1.  Does
 * not buffer; reads one byte at a time.  This is inefficient but allows mixing
 * of blocking and non-blocking IO.
 */
public class ChannelInputStream extends InputStream {
    ByteBuffer _buf=ByteBuffer.allocate(1);
    SocketChannel _channel;

    /**
     * Creates a new input stream that delegates to channel.  No other thread
     * may read from channel while this is in use.  Configures channel for
     * blocking IO.
     * @param channel the delegate for IO operations
     * @exception IOException couldn't make channel non-blocking
     */
    public ChannelInputStream(SocketChannel channel) throws IOException {
        this._channel=channel;
        _channel.configureBlocking(true);
    }

    public int read() throws IOException {
        repOk();
        try {
            int n=_channel.read(_buf);
            if (n<0)
                return n;   //EOF
            else if (n==1)
                return _buf.get(0);
            else {
                Assert.that(false, "Read unexpected number of bytes");
                return -1;
            }
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
