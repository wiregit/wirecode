
// Commented for the Learning branch

package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.limegroup.gnutella.io.BufferUtils;

/**
 * Token is the base class for objects like BELong and BEList, which read bencoded data from a channel and parse it into a Java object.
 * 
 * The Token class also contains the static method getNextToken().
 * Call Token.getNextToken(ReadableByteChannel) to get a type-specific Token object that will parse the next bencoded sentence.
 * 
 * For instance, imagine the channel c has the bencoded data "5:hello" waiting to be read from it.
 * Token.getNextToken(c) will read the first character, "5", telling it the next bencoded sentence is a string.
 * It will create a new BEString object, and return it.
 * Call handleRead() on this object to get it to read the rest of the bencoded sentence from the channel.
 * Call isDone() on it to see if it's done or if it needs more data.
 * When it's done, call getResult() to get the String "hello" that the BEString Token parsed and made.
 * 
 * The inside of the parse() method shows how to use a Token object to parse bencoded data:
 * 
 *   Token t = getNextToken(new BufferChannel(data)); // Read the first letter and make the right kind of parsing object
 *   t.handleRead();                                  // Tell it read the bencoded sentence
 *   Object o = t.getResult();                        // Have it return the Java object it made
 * 
 * Bencoding is the simple and extensible data format BitTorrent uses.
 * More information about bencoding is on the Web at:
 * http://en.wikipedia.org/wiki/Bencoding
 * http://www.bittorrent.org/protocol.html in the section titled "The connectivity is as follows".
 */
public abstract class Token {

	/** -1, this is a Token object. */
    protected static final int INTERNAL = -1;
    /** 0, this is a BELong object reading bencoded data like "i87e" and parsing it into a number. */
    public static final int LONG = 0;
    /** 1, this is a BEString object reading bencoded data like "5:hello" and parsing it into a String. */
    public static final int STRING = 1;
    /** 2, this is a BEList object reading bencoded data that starts "i", has a list of bencoded elements, and ends "e". */
    public static final int LIST = 2;
    /** 3, this is a BEDictionary object reading bencoded data that starts "d", has keys and values, and ends "e". */
    public static final int DICTIONARY = 3;

    /** "ISO-8859-1", convert a String object to a byte array with string.getBytes("ISO-8859-1") to use the normal ASCII format for BitTorrent. */
    protected static final String ASCII = "ISO-8859-1";

    /** "i" as an ASCII byte, the start of a bencoded number. */
    protected static final byte I;
    /** "d" as an ASCII byte, the start of a bencoded dictionary. */
    protected static final byte D;
    /** "l" as an ASCII byte, the start of a bencoded list. */
    protected static final byte L;
    /** "e" as an ASCII byte, the end of a number, dictionary, or list. */
    protected static final byte E;
    /** "0" as an ASCII byte, bencoded strings start with a numeral "0" through "9". */
    protected static final byte ZERO;
    /** "9" as an ASCII byte, bencoded strings start with a numeral "0" through "9". */
    protected static final byte NINE;

    // Java runs this code right before control first enters this class
    static {

    	// Make bytes to hold the letters like "i" and "d" that bencoding uses
    	byte i = 0;
        byte d = 0;
        byte l = 0;
        byte e = 0;
        byte zero = 0;
        byte nine = 0;

        try {

        	// Convert a String like "i" into ASCII bytes using "ISO-8859-1" encoding, and get the first byte
        	i = "i".getBytes(ASCII)[0]; // [0] looks at the byte array getBytes() returns, and gets the first byte
            d = "d".getBytes(ASCII)[0];
            l = "l".getBytes(ASCII)[0];
            e = "e".getBytes(ASCII)[0];
            zero = "0".getBytes(ASCII)[0];
            nine = "9".getBytes(ASCII)[0];

        } catch (UnsupportedEncodingException impossible) {}

        // Save the bytes we made in the member variables
        I = i;
        D = d;
        L = l;
        E = e;
        ZERO = zero;
        NINE = nine;
    }

    /**
     * A ByteBuffer with room for a single byte.
     * getNextToken() reads one byte from the channel into this buffer, then sees what letter it is.
     */
    private static final ByteBuffer ONE_BYTE = ByteBuffer.wrap(new byte[1]);

    /**
     * The channel this Token object reads bencoded data from.
     * When you make a new object that extends Token, like a new BEString, you'll give the constructor this source channel.
     */
    protected final ReadableByteChannel chan;

    /**
     * The parsed Java object this Token made from the bencoded data it read.
     * Before this Token object is done reading and parsing bencoded data from its channel, result will be null.
     */
    protected Object result;

    /**
     * Make a new object that can read and parse bencoded data.
     * 
     * @param chan The ReadableByteChannel this new Token object will read bencoded data from
     */
    public Token(ReadableByteChannel chan) {

    	// Save the given channel in this new object so we can use it to read from it later
        this.chan = chan;
    }

    /**
     * The "NIODispatcher" thread will call handleRead() when it's time for this object to read more bencoded data from its channel.
     * Classes that extend Token, like BEString and BEList, have handleRead() methods.
     * 
     * We don't give a Token object bencoded data to decode.
     * Instead, we give it a channel that it can read bencoded data from.
     * Later, we'll call handleRead(), telling it to read more data from its channel.
     * This way, it can read an unlimited amount of data and no calls ever block.
     */
    public abstract void handleRead() throws IOException;

    /**
     * Determine if this object has read a complete bencoded piece of data and parsed it into a Java object.
     * Classes that extend Token, like BEString and BEList, have isDone() methods.
     * 
     * A complete bencoded piece of data is like "5:hello" or "i87e".
     * Objects that extend Token are careful to not read any bytes beyond the bencoded piece they're parsing.
     * When they haven't read the whole thing yet, isDone() returns false.
     * When they have finished, isDone() returns true and you can get the Java object they made and parsed from them.
     * 
     * @return True if this object has read the whole piece of bencoded data, and parsed it into a Java object.
     *         False if this object still needs more bytes from the channel to finish.
     */
    protected abstract boolean isDone();

    /**
     * Find out what kind of object that extends Token this is.
     * For instance, if this is a BEString object, getType() will call BEString.getType() which returns 1, Token.STRING.
     * 
     * @return -1 Token.INTERNAL, the type code for the Token base class
     */
    public int getType() {

    	// If you make a Token object, token.getType() will return -1, Token.INTERNAL
    	return INTERNAL;
    }

    /**
     * Get the Java object this Token object made from the bencoded data it read and parsed.
     * 
     * @return The Java Object we turned the bencoded data into.
     *         null if we haven't read enough bencoded data from our channel to make it yet.
     */
    public Object getResult() {

    	// Only return result if isDone() returns true
        if (!isDone()) return null;
        return result;
    }

    /**
     * TERMINATOR is a Token you'll get when there are no more tokens in a list.
     * 
     * Methods in the bencoding class return a list of decoded Java objects like this:
     * Each time you call the method, you'll get the next object in the list.
     * When you've gotten all the objects, the method will return this TERMINATOR object.
     * getNextToken() below is one of the methods that does this.
     */
    static final EndElement TERMINATOR = new EndElement();

    /**
     * TERMINATOR above is the only EndElement object the program makes.
     * 
     * The TERMINATOR EndElement is a Token object with unusual properties.
     * It has no channel it can read bencoded data from.
     * The Java object it read and parsed is itself.
     * Right from the start, it's done reading bencoded data and making its Java object.
     */
    private static class EndElement extends Token {

    	/** Make the TERMINATOR EndElement object. */
    	EndElement() {

    		// TERMINATOR has no channel to read from
            super(null);

            // The object TERMINATOR parsed is itself
            result = this;
        }

    	/** If you tell TERMINATOR to read from its channel, it won't do anything. */
    	public void handleRead() throws IOException {}

    	/**
    	 * Determine if TERMINATOR is finished reading bencoded data from its channel and parsing it into a Java object.
    	 * 
    	 * @return true, there is no channel to read from, no data to parse, and no object to make
    	 */
    	protected boolean isDone() {

    		// There is no data to parse
    		return true;
        }
    }

    /**
     * Read the next bencoded object from the channel, returning a type-specific object like a BEString that can read, parse, and make the object.
     * 
     * The object getNextToken() returns won't be done making it's parsed Java object right away.
     * Call handleRead() on it to get it to read more bencoded data from its channel.
     * Call isDone() on it to see if it's done reading a complete bencoded sentence.
     * Then, call getResult() on it to get the Java object like a String that it made.
     * 
     * @param chan The channel we can read bencoded data from
     * @return     An object that extends Token like BEList that can read the bencoded sentence and create the Java object
     */
    public static Token getNextToken(ReadableByteChannel chan) throws IOException {

    	/*
    	 * There's some bencoded data in the given chanel for us to read and parse.
    	 * It might be a string like "5:hello", or a list that starts "l", has other elements, and ends "e".
    	 * 
    	 * First, it reads a single byte from the channel.
    	 * This is going to be a number like "5", or a letter that identifies a type like "l".
    	 * 
    	 * Based on what letter it reads, it hands off control to a type specific constructor.
    	 * If it's a "d" for dictionary for instance, it gives the channel to the BEDictionary constructor.
    	 * 
    	 * The type-specific constructor doesn't try to read any more bencoded data from the channel.
    	 * It just saves the letter we read, the channel it can get more from, and returns the new object.
    	 * 
    	 * Later, the "NIODispatcher" thread will call handleRead() on the type-specific object.
    	 * This will make it read more bencoded data from its channel until it's read the whole bencoded piece of data.
    	 */

        try {

        	// Read a single character from the channel, like "i" or "7", telling us what type of bencoded data is next
            int read = chan.read(ONE_BYTE); // The read() method will fill the ONE_BYTE buffer, which only holds 1 byte
            if (read == 0) return null; // If the channel didn't give us the 1 byte we need to determine the type, return null
            if (read == -1) throw new IOException("channel closed while trying to read next token");

        // Always run this code after the code in the try block, if there is or isn't an exception
        } finally {

        	// Mark the ByteBuffer empty for the next time
        	ONE_BYTE.clear();
        }

        // Get the byte we read, like "i" or "5", which identifies what kind of bencoded data chunk is in the channel next
        byte b = ONE_BYTE.array()[0];

        // If the letter is "i", make a BELong() object, for "d" make a BEDictionary, and so on
        if      (b == I) return new BELong(chan);
        else if (b == D) return new BEDictionary(chan);
        else if (b == L) return new BEList(chan);
        else if (b == E) return Token.TERMINATOR; // If we read an e, return the TERMINATOR, this marks the end of a list of bencoded elements
        else if (b >= ZERO && b <= NINE) return new BEString(b, chan); // Bencoded strings start with their length, which starts "0" through "9"
        else throw new IOException("unrecognized token type " + (char)b);
    }

    /**
     * Parse bencoded data in a byte array into a Java object.
     * For instance, if you pass parse() a byte array like "5:hello", it will return the String "hello".
     * Use this when you have all the bencoded data right here, and don't need to read some from the wire and read some more later.
     * 
     * @param data A byte array with a complete bencoded object.
     * @return     An Java object we decoded it into.
     *             null if the byte array didn't contain a complete bencoded object.
     */
    public static Object parse(byte[] data) throws IOException {

    	// Read the first letter like "l" to see what's next, and make a type-specific object like a BEList that can parse it
        Token t = getNextToken(new BufferChannel(data));
        if (t == null) return null; // The channel couldn't even give 1 byte

        // Tell t to read from its channel and parse the data it reads
        t.handleRead();

        // Get the Java object t made, and return it
        return t.getResult();
    }

    /**
     * A BufferChannel wraps a byte array, putting a ReadableByteChannel interface on it.
     * Only the parse() method above makes one.
     */
    private static class BufferChannel implements ReadableByteChannel {

    	/** A ByteBuffer with the data this BufferChannel holds. */
    	private final ByteBuffer src;

        /**
         * Makes a new BufferChannel, wrapping a byte array of data in a ReadableByteChannel interface.
         * 
         * @param data A byte array with the data
         */
        BufferChannel(byte[] data) {

        	// Make a new ByteBuffer with the given byte array
            src = ByteBuffer.wrap(data);
        }

        /**
         * Move as many bytes as possible from this channel into the given dst buffer.
         * 
         * @param dst The destionation ByteBuffer this object will put its data into.
         * @return    The number of bytes this read() method deposited there.
         *            0 if it didn't write any bytes because dst has no space.
         *            -1 if it didn't write any bytes because we're out.
         */
        public int read(ByteBuffer dst) throws IOException {

        	// Move as much data as possible from the source buffer this object keeps to the given destination buffer
            int ret = BufferUtils.transfer(src, dst, false); // Returns the number of bytes it moved

            // If that didn't move any data because we're out, return -1, the code for end of file
            if (ret == 0 && !src.hasRemaining()) return -1; // Real channels return this when their socket connections are closed

            // Return the number of bytes we deposited in the given buffer
            return ret;
        }

        /**
         * Does nothing.
         * This BufferChannel object has no channel to close.
         */
        public void close() throws IOException {}

        /**
         * Always returns true.
         * This BufferChannel object doesn't have a channel, so it can't be closed.
         */
        public boolean isOpen() {

        	// Return true because there is no channel
            return true;
        }
    }
}
