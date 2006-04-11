
package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
    
    protected static final String ASCII = "ISO-8859-1";
    protected static final byte I,D,L,E,ZERO,NINE;
    static {
        byte i = 0;
        byte d = 0;
        byte l = 0;
        byte e = 0;
        byte zero = 0;
        byte nine = 0;
        try {
            i = "i".getBytes(ASCII)[0];
            d = "d".getBytes(ASCII)[0];
            l = "l".getBytes(ASCII)[0];
            e = "e".getBytes(ASCII)[0];
            zero = "0".getBytes(ASCII)[0];
            nine = "9".getBytes(ASCII)[0];
        } catch (UnsupportedEncodingException impossible) {
            // connect to error service eventually
        }
        I = i; D = d; L = l; E = e; ZERO = zero; NINE = nine;
    }
    
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
        if (b == I)
            return new BELong(chan);
        else if (b == D)
            return new BEDictionary(chan);
        else if (b == L)
            return new BEList(chan);
        else if (b == E)
            return Token.TERMINATOR;
        else if (b > ZERO && b <= NINE)
            return new BEString(b,chan);
        else
            throw new IOException("unrecognized token type "+(char)b);
    }

    /**
     * Utility method which parses a BEncoded object from a byte[].
     * It assumes the array contains the entire object.
     */
    public static Object parse(byte []data) throws IOException {
        Token t = getNextToken(new BufferChannel(data));
        if (t == null)
        	return null;
        t.handleRead();
        return t.getResult();
    }
    
    private static class BufferChannel implements ReadableByteChannel {
        private final ByteBuffer src;
        
        BufferChannel(byte []data) {
            src = ByteBuffer.wrap(data);
        }
        
        public int read(ByteBuffer dst) throws IOException {
            if (!src.hasRemaining())
                return -1;
            int position = src.position();
            src.limit(Math.min(src.capacity(),src.position() + dst.remaining()));
            dst.put(src);
            src.limit(src.capacity());
            return src.position() - position;
        }
        
        public void close() throws IOException {}
        public boolean isOpen() {
            return true;
        }
        
        
    }
}
