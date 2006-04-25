package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A bencoding Token that represents a string element of bencoded data.
 * 
 * In BitTorrent's bencoding, a string data on the wire looks like "5:hello".
 * The length comes first, then a colon, then that number of characters.
 * Bencoded strings hold ASCII text, or data of any format.
 * 
 * If bencoded data starts "0" through "9", it's a string.
 * A BEString object can read the rest of it.
 * Calling beString.getToken() returns a Java String with the payload text.
 * 
 * TODO: While we're parsing, we're putting data in a byte array, then at the end, we convert it into a String, is this what we want?
 */
class BEString extends Token {

	/**
	 * The largest bencoded string we'll read.
	 * .torrent files don't have a maximum size, so for now this limit it set to 1 MB.
	 * TODO: Find a proper way to deal with this limit.
	 */
	private static final int MAX_STRING_SIZE = 1024 * 1024;

    /** The first byte of the length of the string that was read from the channel. */
    private final byte firstSizeByte;

    /** Token that will be used to parse the string length at the start of the bencoded data. */
    private BELong sizeToken;

    /** The parsed length of the string. */
    private int size = -1; // -1 because we haven't read the length prefix yet

    /** Buffer used for internal storage. */
    private ByteBuffer buf;

    /** Empty Buffer to point a reference at. */
    private static final ByteBuffer EMPTY_STRING = ByteBuffer.allocate(0);

    /** Separates the length from the string in the data of a bencoded string. */
    final static byte COLON;
    static {
        byte colon = 0;
        try {
            colon = ":".getBytes(ASCII)[0];
        } catch (UnsupportedEncodingException impossible) {
            // hook to ErrorService
        }
        COLON = colon;
    }

    /**
     * Makes a new BEString Token ready to parse bencoded string data from a given ReadableByteChannel.
     * 
     * @param firstChar The first byte we already read from the channel, it was "0" through "9" indicating this is a string
     * @param chan      The ReadableByteChannel the caller read the first character from, and we can read the remaining characters from
     */
    BEString(byte firstChar, ReadableByteChannel chan) {
        super(chan);
        this.firstSizeByte = firstChar; // We'd stuff this character back in the channel if we could
    }

    // Notification that this can read more bencoded string data from its channel
    public void handleRead() throws IOException {

    	// If we haven't read the whole length prefix yet, try to read more of it
        if (size == -1 && !readSize())
            return; // Don't do more until the next read notification

        if (size == 0)
        	return; // There is no string data to read
        if (!buf.hasRemaining()) // Once we've read the whole string, we shouldn't get another read notification
            throw new IllegalStateException("Token is done - don't read to it");

        // Read bencoded data from the channel until we're out of space or our channel is out of data
        int read = 0;
        while (buf.hasRemaining() && (read = chan.read(buf)) > 0);
        if (read == -1 && buf.hasRemaining())
            throw new IOException("closed before end of String token");
        if (!buf.hasRemaining()) // We've read all the string data
            result = new String((byte[])result, "ISO-8859-1");
        // TODO: Use Token.ASCII instead of "ISO-8859-1" above
    }

    /**
     * Reads the length prefix at the start of a bencoded string.
     * 
     * @return True if it got it all and set size, false to call it again to keep reading more
     */
    private boolean readSize() throws IOException {

    	/*
    	 * A bencoded string is like "17:this is the text".
    	 * The length of "this is the text", 17, is written at the start before the colon.
    	 * The first step is to read the length.
    	 * To do this, readSize() makes a new BELong object.
    	 * We give it our channel to read from and tell it to stop when it gets to a ":".
    	 * We call handleRead() on it to get it to read bencoded data from our channel.
    	 * When it's read the ":", it returns the number it read and parsed as a Long object, and control enters the if statement.
    	 */

    	if (sizeToken == null)
            sizeToken = new BELong(chan, COLON, firstSizeByte);
        sizeToken.handleRead();
        Long l = (Long)sizeToken.getResult();
        if (l != null) { // Same as size == -1
            sizeToken = null; // We don't need the object that read the length anymore
            long l2 = l.longValue();

            // Valid length
            if (l2 > 0 && l2 < MAX_STRING_SIZE) {

            	size = (int)l2;
                result = new byte[size];
                buf = ByteBuffer.wrap((byte[])result);
                return true;

            // The bencoded string data is "0:", this is valid
            } else if (l2 == 0) {

            	size = 0;
            	buf = EMPTY_STRING;
            	result = "";
            	return true;

            } else
                throw new IOException("invalid string length"); // Too big
        } else
            return false; // We're still reading the size, try again next time we get called
    }

    /**
     * Determines if this is finished reading bencoded data from its channel and parsing it into a String object.
     * 
     * @return True if it is, false if it needs more read notifications to finish
     */
    protected boolean isDone() {

    	// We're only done if we parsed the size to make a buffer that big, and then filled it
        return buf != null && !buf.hasRemaining();
    }

    /**
     * Tells that this BEString Token object is parsing a bencoded string into a Java String.
     * 
     * @return Token.STRING
     */
    public int getType() {
        return STRING;
    }
}
