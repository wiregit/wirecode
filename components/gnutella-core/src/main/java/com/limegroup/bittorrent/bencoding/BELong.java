package com.limegroup.bittorrent.bencoding;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A token used for the parsing of a Long value.
 */
class BELong extends Token {
    
    private static final byte MINUS;
    static {
        byte minus = 0;
        try {
            minus = "-".getBytes(ASCII)[0];
        } catch (UnsupportedEncodingException impossible) {
            // hook to ErrorService
        }
        MINUS = minus;
    }
    
    /** Storage for the value of the token */
    private StringBuffer sb = new StringBuffer();
    private byte [] currentByte = new byte[1];
    private ByteBuffer buf = ByteBuffer.wrap(currentByte);

    /** -1 for negative values, 0 for 0, 1 for positive values */
    private int multiplier = 1; 
    
    /** whether this token has been parsed */
    private boolean done;
    
    /** The terminating character used to parse this token */
    private final byte terminator;
    
    /**
     * Creates a new Token ready to parse a Long using the default terminating
     * character.
     */
    BELong(ReadableByteChannel chan) {
        this(chan, E, (byte)0);
    }
    
    /**
     * Creates a new Token read to parse a Long using a custon terminating
     * character
     * @param terminator the character that will mark the end of the value
     * @param firstByte the first character of the value, if read.
     */
    BELong(ReadableByteChannel chan, byte terminator, byte firstByte) {
        super(chan);
        this.terminator = terminator;
        if (firstByte != 0) {
            if (firstByte <= ZERO || firstByte > NINE)
                throw new IllegalArgumentException("invalid first byte");
            sb.append(firstByte - ZERO); 
        }
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
            
            if (currentByte[0] < ZERO || currentByte[0] > NINE) {
                if (currentByte[0] == MINUS && sb.length() == 0 && multiplier != -1)
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
            else if (currentByte[0] == ZERO) {
                switch (sb.length()) {
                case 0 :
                    if (multiplier == -1) throw new IOException("negative 0");
                    multiplier = 0;
                    break;
                case 1 :
                    if (multiplier == 0) throw new IOException("leading 0s");
                }
                sb.append(0);
            }
            else {
                if (multiplier == 0)
                    throw new IOException("leading 0s - wrong");
                sb.append(currentByte[0] - ZERO);
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
