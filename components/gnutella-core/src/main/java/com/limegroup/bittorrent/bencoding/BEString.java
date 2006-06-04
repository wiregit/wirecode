
// Commented for the Learning branch

package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A BEString object reads bencoded data like "5:hello" from a channel and parses it into a String object.
 * 
 * In BitTorrent's bencoding, a string data on the wire looks like "23:this is the string data".
 * The length comes first, then a colon, then that number of characters.
 * Bencoded strings hold ASCII text, or data of any format.
 * 
 * If bencoded data starts "0" through "9", it's a string.
 * Make a new BEString object, giving it the digit you read and the channel where it can get the rest of them.
 * It will use an internal BELong object to parse the length, make a ByteBuffer exactly the right size, and move the string data across.
 * Call handleRead() when you know there's more bencoded data in the channel.
 * When isDone() returns true, call getResult() to get the String with the data.
 */
class BEString extends Token {

	/** 1 MB of bytes, the largest bencoded string we'll read. */
	private static final int MAX_STRING_SIZE = 1024 * 1024;

    /**
     * The first numeral in the length of the string.
     * We may have already read this from the channel to determine what kind of bencoded data is next in the channel.
     */
    private final byte firstSizeByte;

    /** A BELong object we'll use to parse the length of the string at the start of the bencoded data. */
    private BELong sizeToken;

    /** The length of the string. */
    private int size = -1; // -1 because we haven't read the length prefix yet

    /** The ByteBuffer handleRead() will use to hold all the string data. */
    private ByteBuffer buf;

    /** An empty ByteBuffer to reference when the bencoded data is "0:", for the empty string. */
    private static final ByteBuffer EMPTY_STRING = ByteBuffer.allocate(0);

    /** ":", A colon separates the length from the string, like "5:hello". */
    final static byte COLON;

    // Java runs this before control enters the first method here
    static {

    	// Set COLON to the ASCII byte of the ":" character
        byte colon = 0;
        try { colon = ":".getBytes(ASCII)[0]; } catch (UnsupportedEncodingException impossible) {}
        COLON = colon;
    }

    /**
     * Make a new BEString object ready to read and parse a bencoded string like "5:hello" into a Java String like "hello".
     * 
     * @param firstChar The first character of the bencoded string.
     *                  It's a numeral, "0" through "9".
     *                  We read it from the channel, and it told us this is a bencoded string.
     * @param chan      The channel we're reading bencoded data from.
     */
    BEString(byte firstChar, ReadableByteChannel chan) {

    	// Save the given channel in this new object
    	super(chan);

    	// Save the first size byte the caller already read from the channel
        this.firstSizeByte = firstChar; // We'd stuff it back into the channel if we could
    }

    /**
     * The "NIODispatch" thread will call this handleRead() method when this BEString object can read more bencoded data from the channel we gave it.
     * 
     * handleRead() calls readSize(), which parses the length prefix like "123:", sets the size member variable, and makes a buffer exactly the right size.
     * It calls chan.read(buf), which copies data from the channel into buf.
     * When buf is full, isDone() will start returning true, and getResult() will return the whole String.
     */
    public void handleRead() throws IOException {

    	// If we haven't read the whole length prefix like "123:" yet, try to read more
        if (size == -1 && // If we don't know how long the string is yet
        	!readSize())  // Call readSize() to read the length prefix, which returns true when it reads the ":" and sets size
        	return;       // If readSize() didn't finish, try calling it again the next time

        // readSize() read "0:", the complete bencoded string for blank, we're done
        if (size == 0) return;

        // No one should call handleRead() after we've read our whole string from the channel
        if (!buf.hasRemaining()) throw new IllegalStateException("Token is done - don't read to it");

        // Move data from the channel into buf until buf is full and we're done, or the channel doesn't have any more data right now
        int read = 0;
        while (buf.hasRemaining() &&       // readSize() made buf exactly the right size, if our buffer still has space
        	(read = chan.read(buf)) > 0) ; // Move data from the channel to the buffer, if we got some, loop to do it again

        // Throw an exception if the channel closed before we could read the whole string
        if (read == -1 &&       // If channel.read() returned -1, indicating the connection it represents is lost, and
        	buf.hasRemaining()) // Our buffer still has space, we're not done reading the whole thing
            throw new IOException("closed before end of String token");
    }

    /**
     * Read the length prefix at the start of a bencoded string, like "123:".
     * Doesn't read any further than that.
     * Turns the numerals into a number, and saves it in the size member variable.
     * Sizes result and buf to hold the string data.
     * 
     * @return True if we've read the whole length prefix, and are ready to read the string data.
     *         False if handlRead() should call readSize() some more so we can keep reading numerals to reach the ":".
     */
    private boolean readSize() throws IOException {

    	/*
    	 * A bencoded string is like "17:this is the text".
    	 * The length of "this is the text", 17, is written at the start before the colon.
    	 * 
    	 * The first step is to read the length.
    	 * To do this, readSize() makes a new BELong object.
    	 * We give it our channel to read from and tell it to stop when it gets to a ":".
    	 * 
    	 * We call handleRead() on it to get it to read bencoded data from our channel.
    	 * When it's read the ":", it returns the number it read and parsed as a Long object, and control enters the if statement.
    	 */

    	// The first time this runs, make a BELong object that will read numerals until it gets to a ":"
    	if (sizeToken == null) sizeToken = new BELong(chan, COLON, firstSizeByte); // Give it the first numeral we already read

    	// Tell our numeral reader to read more
        sizeToken.handleRead(); // It will read up to the ":", but no further

        // Find out if our numeral reader has read the whole length yet
        Long parsedLong = (Long)sizeToken.getResult(); // If it's read all the numerals and the ":", getResult() will return a Long with the number
        if (parsedLong != null) { // It's done, we set size to something other than -1 here, so handleRead() won't call readSize() again

        	// Free the numeral reader object
            sizeToken = null; // We don't need the object anymore
            long length = parsedLong.longValue();

            // Valid length
            if (length > 0 && length < MAX_STRING_SIZE) {

            	// Set the member variables to read the string data next
            	size = (int)length;                    // Now we know how long the data that comes next is
                result = new byte[size];               // Make a byte array that can hold it exactly
                buf = ByteBuffer.wrap((byte[])result); // Wrap a ByteBuffer around it to use it that way also

                // Return true to indicate we finished reading the length prefix
                return true;

            // The bencoded string data is "0:", this is valid
            } else if (length == 0) {

            	// Set the member variables of this BEString object to represent an empty String
            	size = 0;             // No length
            	buf = EMPTY_STRING;   // Point buf at the empty ByteBuffer we made
            	result = new byte[0]; // Have getResult() return an empty byte array

                // Return true to indicate we finished reading the length prefix
            	return true;

            // The length is negative, or too big
            } else {

            	// Throw an exception instead of continuing
            	throw new IOException("invalid string length");
            }

        // sizeToken hasn't finished reading the length prefix like "123:" yet
        } else {

        	// Return false to indicate we're still reading the length prefix, and handleRead() should call readSize() again
        	return false;
        }
    }

    /**
     * Find out if this BEString object is finished reading its bencoded string from its channel, and has parsed it into a String object.
     * 
     * @return True, this object has read the entire bencoded string from the channel and getResult() will return a String.
     *         False, call the handleRead() method more to get this object to read more bencoded data from its channel.
     */
    protected boolean isDone() {

    	// We're only done if we parsed the size to make a buffer that big, and then filled it
        return buf != null && !buf.hasRemaining();
    }

    /**
     * Determine what kind of Token object this is, and what kind of Java object it will parse.
     * 
     * @return Token.STRING, the code number for a BEString object that will produce a Java String
     */
    public int getType() {

    	// Return the Token.STRING code
        return STRING;
    }
}
