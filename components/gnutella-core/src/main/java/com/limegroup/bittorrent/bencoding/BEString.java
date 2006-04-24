package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A token that represents a String object.
 */
class BEString extends Token {

	/**
	 * The largest bencoded string we'll read.
	 * .torrent files don't have a maximum size, so for now this limit it set to 1 MB.
	 * TODO: Find a proper way to deal with this limit.
	 */
	private static final int MAX_STRING_SIZE = 1024 * 1024;

    /** The first byte of the length of the string. */
    private final byte firstSizeByte;

    /** Token that will be used to parse how long the string is */
    private BELong sizeToken;

    /** The parsed length of the string */
    private int size = -1;

    /** Buffer used for internal storage */
    private ByteBuffer buf;

    private static final ByteBuffer EMPTY_STRING = ByteBuffer.allocate(0);

    /** Separates the length from the string. */
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
     * Make a new BEString Token ready to parse a bencoded string.
     * 
     * @param firstChar The first byte we already read from the channel, it was "0" through "9" indicating this is a string
     * @param chan      The ReadableByteChannel the caller read the first character from, and we can read the remaining characters from
     */
    BEString(byte firstChar, ReadableByteChannel chan) {
        super(chan);
        this.firstSizeByte = firstChar; // This character is part of the bencoded data, but it's not in the channel anymore
    }

    public void handleRead() throws IOException {
        if (size == -1 && !readSize()) 
            return; // try next time.
        if (size == 0)
        	return;
        if (!buf.hasRemaining())
            throw new IllegalStateException("Token is done - don't read to it");

        int read = 0;
        while(buf.hasRemaining() && (read = chan.read(buf)) > 0);

        if (read == -1 && buf.hasRemaining())
            throw new IOException("closed before end of String token");

        if (!buf.hasRemaining())
            result = new String((byte[])result, "ISO-8859-1");
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private boolean readSize() throws IOException {
        if (sizeToken == null)
            sizeToken = new BELong(chan, COLON, firstSizeByte);

        sizeToken.handleRead();
        Long l = (Long) sizeToken.getResult();
        if (l != null) {
            sizeToken = null; //don't need this object anymore
            long l2 = l.longValue();
            if (l2 > 0 && l2 < MAX_STRING_SIZE) {
                size = (int)l2;
                result = new byte[size];
                buf = ByteBuffer.wrap((byte[])result);
                return true;
            }
            else if (l2 == 0) {
            	size = 0;
            	buf = EMPTY_STRING;
            	result = "";
            	return true;
            } else
                throw new IOException("invalid string length");
        } else
            return false; // continue next signal.
    }

    protected boolean isDone() {
        return buf != null && !buf.hasRemaining();
    }

    public int getType() {
        return STRING;
    }
}
