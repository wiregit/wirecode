package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * The Token class is the base class for classes that represent pieces of bencoded data.
 * The code here can read bencoded data, and parse it into objects that extend Token, like BEString and BEList.
 * 
 * How to parse bencoded data:
 * (1) Create a Token, giving it a ReadableByteChannel for it to get data from.
 * (2) When you discover there is data on the channel, call token.handleRead() to have the Token read the data from its channel.
 * (3) Call token.getResult() to see if it returns an object.
 * (4) Call getType() on the object to see what kind of object it is.
 * 
 * BitTorrent uses a simple and extensible data format called bencoding.
 * More information about bencoding is on the Web at:
 * http://en.wikipedia.org/wiki/Bencoding
 * http://www.bittorrent.org/protocol.html in the section titled "The connectivity is as follows".
 * 
 * Token should implement an interface from the com.limegroup.gnutella.io package, like ReadObserver or ChannelReadObserver.
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
     * Read the next bencoded element from our channel, parse it into an object like BEList that extends Token, and return it.
     * 
     * @return The object we read and parsed.
     *         null if the channel didn't give us a complete bencoded element.
     * @throws IOException if a read from the channel throws.
     */
    public static Token getNextToken(ReadableByteChannel chan) throws IOException {

    	// Read the next byte from our channel
        try {
            int read = chan.read(ONE_BYTE); // Read one byte from our channel into the ONE_BYTE ByteBuffer
            if (read == 0)
                return null; // The channel gave us no data, so we have no parsed object to return
            if (read == -1)
                throw new IOException("channel closed while trying to read next token");
        } finally {
            ONE_BYTE.clear(); // Mark the ByteBuffer empty, doesn't erase the byte we read there
        }

        // Sort on what letter it is, calling a constructor like BEDictionary() and returning the object it makes
        byte b = ONE_BYTE.array()[0]; // Get the byte we read
        if (b == I) // "i", this is the start of a bencoded number
            return new BELong(chan);
        else if (b == D) // "d", this is the start of a bencoded dictionary
            return new BEDictionary(chan);
        else if (b == L) // "l", this is the start of a bencoded list
            return new BEList(chan);
        else if (b == E) // "e", this is the end of something
            return Token.TERMINATOR; // Return the TERMINATOR Token object
        else if (b >= ZERO && b <= NINE) // A number "0" through "9", this is the start of a length before a string
            return new BEString(b, chan);
        else
            throw new IOException("unrecognized token type " + (char)b);
    }

    /**
     * Parse bencoded data in a byte array into an object that extends Token.
     * 
     * @param data A byte array with a complete bencoded object.
     * @return     An object that extends Token like BEList.
     *             null if the byte array didn't contain a complete bencoded object.
     */
    public static Object parse(byte[] data) throws IOException {
        Token t = getNextToken(new BufferChannel(data)); // Wrap the array in a BufferChannel to give it to getNextToken(), which reads the first letter like "l"
        if (t == null)
        	return null; // No data
        t.handleRead(); // Call handleRead() to get the object getNextToken() made to read the rest of the data
        return t.getResult(); // Get and return the object it made
    }

    /**
     * A BufferChannel wraps a byte array, putting a ReadableByteChannel interface on it.
     */
    private static class BufferChannel implements ReadableByteChannel {

    	/** A ByteBuffer with the data this BufferChannel holds. */
    	private final ByteBuffer src;

        /**
         * Make a new BufferChannel, wrapping a byte array of data in a ReadableByteChannel interface.
         * 
         * @param data A byte array with the data
         */
        BufferChannel(byte[] data) {
            src = ByteBuffer.wrap(data); // Make a ByteBuffer from the given data, and save it
        }

        /**
         * Call read() to get the data from this BufferChannel object.
         * 
         * @param dst The destination ByteBuffer for this method to put the data the caller is reading from us
         */
        public int read(ByteBuffer dst) throws IOException {
            if (!src.hasRemaining())
                return -1; // Return -1, the code for end of file
            int position = src.position();
            src.limit(Math.min(src.capacity(), src.position() + dst.remaining())); // Set the limit to not overflow the given destination buffer
            dst.put(src); // Copy data from src to dst
            src.limit(src.capacity()); // Set the limit back to the end
            return src.position() - position; // Return the number of bytes we copied from ourselves to the destination buffer
        }

        /** Close this BufferChannel, does nothing. */
        public void close() throws IOException {}

        /**
         * Determine if this BufferChannel is open.
         * 
         * @return Always returns true
         */
        public boolean isOpen() {
            return true;
        }
    }
}
