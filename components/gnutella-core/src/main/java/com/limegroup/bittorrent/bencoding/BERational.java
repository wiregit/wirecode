package com.limegroup.bittorrent.bencoding;

import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;


public class BERational extends NumberToken<Double> {
    BERational(ReadableByteChannel chan) {
        super(chan);
    }
    
    public int getType() {
        return RATIONAL;
    }
    
    protected Double getResult(BigInteger rawValue) {
        double ret = Double.longBitsToDouble(rawValue.longValue());
        return ret * multiplier;
    }
}
