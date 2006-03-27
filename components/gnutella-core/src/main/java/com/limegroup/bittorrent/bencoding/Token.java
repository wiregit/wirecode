
package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;


/**
 * this implements http://www.bittorrent.com/protocol.html  the part about bencoding
 * 
 * Use:  create a Token, delegate any handleRead() calls until getResult() returns
 * a non-null value, cast the non-null value to whatever getType() returns.  Continue
 * processing the channel as usual because no extra data will be read or buffered anywhere
 * in the parser.
 * 
 * this class will eventually implement some interface from the IO package.. 
 * probably ReadObserver or ChannelReadObserver
 */

public abstract class Token {
    
    /**
     * Different token types we understand.
     */
    protected static final int INTERNAL = -1;
    public static final int LONG = 0;
    public static final int STRING = 1;
    public static final int LIST = 2;
    public static final int DICTIONARY = 3; // cast to java.util.Map
    
    private static final ByteBuffer ONE_BYTE = ByteBuffer.wrap(new byte[1]);
    
    /** 
     * The channel to read the token from.
     */
    protected final ReadableByteChannel chan;
    
    /**
     * Reference where the parsed result will be stored.
     */
    protected Object result;
    
    
    public Token(ReadableByteChannel chan) {
        this.chan = chan;
    }
    
    public abstract void handleRead() throws IOException;
    
    protected abstract boolean isDone();
    
    /**
     * @return the type of the element, for easer casting.
     */
    public int getType() {
        return INTERNAL;
    }
    
    /**
     * @return the parsed element, null if its not parsed yet.
     */
    public Object getResult() {
        if (!isDone())
            return null;
        return result;
    }
    
    static final EndElement TERMINATOR = new EndElement();
    private static class EndElement extends Token {
        EndElement() {
            super(null);
            result = this;
        }
        public void handleRead() throws IOException {}
        protected boolean isDone() {
            return true;
        }
    }
    
    /**
     * Gets a bencoded element from a ReadableByteChannel.  
     * 
     * @return a Token object that will represent the next element or null
     * if there wasn't enough data available in the channel to determine what
     * the next element would be 
     * 
     * @throws IOException if a read from the channel throws.
     */
    public static Token getNextToken(ReadableByteChannel chan) throws IOException {
        try {
            int read = chan.read(ONE_BYTE);
            if (read == 0) 
                return null;
            if (read == -1)
                throw new IOException("channel closed while trying to read next token");
        } finally {
            ONE_BYTE.clear();
        }
        
        byte b = ONE_BYTE.array()[0];
        switch (b) {
        case (byte)'i':
            return new BELong(chan);
        case (byte)'l':
            return new BEList(chan);
        case (byte)'d':
            return new BEDictionary(chan);
        case (byte)'e':
            return Token.TERMINATOR;
        }
        
        if (b > (byte)'0' && b <= (byte)'9')
            return new BEString(b,chan);
        else
            throw new IOException("unrecognized token type "+(char)b);
    }
}
