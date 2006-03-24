package com.limegroup.bittorrent.bencoding;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * 
 */

class BEString extends Token {
    StringBuffer str;
    final byte firstSizeByte;
    BELong sizeToken;
    int size;
    ByteBuffer buf;
    boolean readColon = false;
    
    public BEString(byte firstChar, ReadableByteChannel chan) {
        super(chan);
        this.firstSizeByte = firstChar;
    }
    
    public void handleRead() throws IOException {
        if (size == 0 && !readSize()) 
            return; // try next time.
        if (!buf.hasRemaining()) 
            throw new IllegalStateException("Token is done - don't read to it");
        
        int read = 0;
        while(buf.hasRemaining() && (read = chan.read(buf)) > 0);
        
        if (read == -1 && buf.hasRemaining())
            throw new IOException("closed before end of String token");
        
        if (!buf.hasRemaining())
            result = new String((byte[]) result);
    }
    
    private boolean readSize() throws IOException {
        if (sizeToken == null) 
            sizeToken = new BELong(chan,':',firstSizeByte);
        
        sizeToken.handleRead();
        Long l = (Long) sizeToken.getResult();
        if (l != null) {
            sizeToken = null; //don't need this object anymore
            long l2 = l.longValue();
            if (l2 > 0 && l2 < 65000) {
                size = (int)l2;
                result = new byte[size];
                buf = ByteBuffer.wrap((byte[])result);
                return true;
            }
            else
                throw new IOException("invalid string length");
        } else
            return false; // continue next signal.
        
    }
    
    protected boolean isDone() {
        return buf != null && !buf.hasRemaining();
    }
    
    public int getType() {
        return STRING;
    }
}
