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
     * Make a new BELong object that can get the number from bencoded data like "i23e" or "5:".
     * 
     * @param chan       A ReadableByteChannel the new BELong can read from to get more data.
     * @param terminator The character we'll look for to mark the end of the numerals, like "e" or ":".
     * @param firstByte  If you already ready the first digit from the channel, pass it here.
     *                   If not, pass 0 for firstByte.
     */
    BELong(ReadableByteChannel chan, byte terminator, byte firstByte) {
        super(chan); // Save the channel this new object will read data from
        this.terminator = terminator; // Save the character we'll watch for to end the number
        if (firstByte != 0) { // The caller a
            if (firstByte < ZERO || firstByte > NINE)
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
                    sb = null;
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
