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

	/** Marks a bencoded token meant for internal use only. */
    protected static final int INTERNAL = -1;
    /** Marks a bencoded number, like "i87e". */
    public static final int LONG = 0;
    /** Marks a bencoded string, like "5:hello". */
    public static final int STRING = 1;
    /** Marks a bencoded list, a list of elements between "l" and "e". */
    public static final int LIST = 2;
    /** Marks a bencoded dictionary, a list of key and value pairs between "d" and "e". You can cast this object to Java.util.Map. */
    public static final int DICTIONARY = 3;

    /**
     * The kind of text encoding to use.
     * We'll use this like string.getBytes(ASCII) so Java will convert String objects to byte arrays using normal ASCII encoding.
     */
    protected static final String ASCII = "ISO-8859-1";

    /** The ASCII byte "i", identifies a bencoded number. */
    protected static final byte I;
    /** The ASCII byte "d", identifies a bencoded dictionary. */
    protected static final byte D;
    /** The ASCII byte "l", identifies a bencoded list. */
    protected static final byte L;
    /** The ASCII byte "e", marks the end of something in bencoding. */
    protected static final byte E;
    /** The ASCII byte "0". */
    protected static final byte ZERO;
    /** The ASCII byte "9". */
    protected static final byte NINE;

    static {

    	byte i = 0;
        byte d = 0;
        byte l = 0;
        byte e = 0;
        byte zero = 0;
        byte nine = 0;

        try {

        	i = "i".getBytes(ASCII)[0]; // Put the character in a String, convert it to an ASCII byte array, and read the first byte
            d = "d".getBytes(ASCII)[0];
            l = "l".getBytes(ASCII)[0];
            e = "e".getBytes(ASCII)[0];
            zero = "0".getBytes(ASCII)[0];
            nine = "9".getBytes(ASCII)[0];

        } catch (UnsupportedEncodingException impossible) {

        	// TODO: connect to the error service
        }

        // Save the bytes we generated in the static final members
        I = i;
        D = d;
        L = l;
        E = e;
        ZERO = zero;
        NINE = nine;
    }

    /** A ByteBuffer with room for a single byte. */
    private static final ByteBuffer ONE_BYTE = ByteBuffer.wrap(new byte[1]);

    /** We'll read bencoded data from this channel. */
    protected final ReadableByteChannel chan;

    /** When we parse some bencoded data into an object, we'll point result at the object we made. */
    protected Object result;

    /**
     * Make a new Token object to represent a bencoded token we will read and parse.
     * 
     * @param chan The ReadableByteChannel we can read bencoded data from
     */
    public Token(ReadableByteChannel chan) {
        this.chan = chan; // Save the given channel
    }

    /**
     * Classes that extend Token should have a handleRead() method.
     * Code will call handleRead() when it's time for us to read from our channel.
     */
    public abstract void handleRead() throws IOException;

    /**
     * Classes that extend Token should have a isDone() method.
     * 
     * @return True if we've read enough bencoded data to parse it into a complete object.
     *         False if we're still waiting to read more bencoded data to finish our object.
     */
    protected abstract boolean isDone();

    /**
     * Find out what kind of bencoded element this is.
     * In this Token base class, getType() returns Token.INTERNAL.
     * Classes that extend Token will return their data type, like Token.STRING or Token.LIST.
     * 
     * @return the type of the element, for easer casting.
     */
    public int getType() {
        return INTERNAL;
    }

    /**
     * Get the object we made from the bencoded data we read and parsed.
     * 
     * @return The Object we parsed.
     *         null if we haven't read enough bencoded data to make it yet.
     */
    public Object getResult() {
        if (!isDone())
            return null;
        return result;
    }

    /**
     * TERMINATOR is a Token object you can use to mark the end of a list of them.
     * Its isDone() method returns true, and its result reference points to itself.
     */
    static final EndElement TERMINATOR = new EndElement();

    /** An EndElement is a Token object that marks the end of a list of Token objects. */
    private static class EndElement extends Token {
    	EndElement() {
            super(null); // No channel to read from
            result = this; // The object we parsed is this one
        }
        public void handleRead() throws IOException {} // handleRead() does nothing
        protected boolean isDone() {
            return true; // Yes, we're done reading and parsing our object
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
            int read = chan.read(ONE_BYTE); // Read one byte from our channel into the ONE_BYTE ByteBuffer
            if (read == 0)
                return null; // The channel gave us no data, so we have no parsed object to return
            if (read == -1)
                throw new IOException("channel closed while trying to read next token");
        } finally {
            ONE_BYTE.clear(); // Mark the ByteBuffer empty, doesn't erase the byte we read there
        }

        byte b = ONE_BYTE.array()[0]; // Get the byte we read
        if (b == I) // "i", this is the start of a bencoded number
            return new BELong(chan);
        else if (b == D) // "d", this is the start of a bencoded dictionary
            return new BEDictionary(chan);
        else if (b == L) // "l", this is the start of a bencoded list
            return new BEList(chan);
        else if (b == E)
            return Token.TERMINATOR;
        else if (b >= ZERO && b <= NINE)
            return new BEString(b, chan);
        else
            throw new IOException("unrecognized token type " + (char)b);
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
