package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;

/**
 * A token used for the parsing of a Long value.
 * Values outside Long.MIN_VALUE and Long.MAX_VALUE throw an IOX.
 */
class BELong extends NumberToken<Long> {
    
    private static final BigInteger MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    
    public BELong(ReadableByteChannel chan, byte terminator, byte firstByte) {
        super(chan, terminator, firstByte);
    }

    public BELong(ReadableByteChannel chan) {
        super(chan);
    }

    public int getType() {
        return LONG;
    }
    
    protected Long getResult(BigInteger rawValue) throws IOException {
        if (rawValue.compareTo(MAX) > 0)
            throw new IOException("too big");
        return rawValue.longValue() * multiplier;
    }
}
