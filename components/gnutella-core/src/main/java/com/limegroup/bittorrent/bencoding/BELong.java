package com.limegroup.bittorrent.bencoding;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * 
 */

class BELong extends Token {
    StringBuffer sb = new StringBuffer();
    byte [] currentByte = new byte[1];
    ByteBuffer buf = ByteBuffer.wrap(currentByte);
    
    int multiplier = 1; // valid values are -1, 0, 1
    boolean done;
    
    private final byte terminator;
    
    public BELong(ReadableByteChannel chan) {
        this(chan, 'e', (byte)' ');
    }
    
    BELong(ReadableByteChannel chan, char terminator, byte firstSizeByte) {
        super(chan);
        this.terminator = (byte)terminator;
        if (firstSizeByte != (byte)' ')
            sb.append((char)firstSizeByte);
    }
    
    public void handleRead() throws IOException {
        if (done)
            throw new IllegalStateException("this token is done.  Don't read it!");
        while(true) {
            try {
                int read = chan.read(buf);
                if (read == -1) 
                    throw new IOException("channel closed before end of integer token");
                else if (read == 0) // no more to read - wait for next signal. 
                    return;
            } finally {
                buf.clear();
            }
            
            if (currentByte[0] < (byte)'0' || currentByte[0] > (byte)'9') {
                if (currentByte[0] == (byte)'-' && sb.length() == 0 && multiplier != -1)
                    multiplier = -1;
                else if (currentByte[0] == terminator && sb.length() != 0) {
                    try {
                        result = new Long(Long.parseLong(sb.toString()) * multiplier);
                    } catch (NumberFormatException impossible) {
                        throw new IOException(impossible.getMessage());
                    }
                    done = true;
                    return;
                }
                else 
                    throw new IOException("invalid integer");
            }
            else if (currentByte[0] == (byte)'0') {
                switch (sb.length()) {
                case 0 :
                    if (multiplier == -1) throw new IOException("negative 0");
                    multiplier = 0;
                    break;
                case 1 :
                    if (multiplier == 0) throw new IOException("leading 0s");
                }
                sb.append((char)currentByte[0]);
            }
            else {
                if (multiplier == 0)
                    throw new IOException("leading 0s - wrong");
                sb.append((char)currentByte[0]);
            }
        }
    }
    
    protected boolean isDone() {
        return done;
    }
    
    public int getType() {
        return LONG;
    }
}
