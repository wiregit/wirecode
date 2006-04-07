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
	 * Maximum size for a string we'll read.  Torrent files do not have limits to their size,
	 * so for now we'll just limit it to 1MB
	 * TODO: find a proper way to deal with this limit.
	 */
	private static final int MAX_STRING_SIZE = 1024*1024*1024;
	
    /** The first byte of the length of the string */
    private final byte firstSizeByte;
    
    /** Token that will be used to parse how long the string is */
    private BELong sizeToken;
    
    /** The parsed length of the string */
    private int size;
    
    /** Buffer used for internal storage */
    private ByteBuffer buf;
    
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
     * Constructs a new Token ready to parse a string
     * 
     * @param firstChar the first character of the token - it will have been
     * read before constructing this object
     */
    BEString(byte firstChar, ReadableByteChannel chan) {
        super(chan);
        this.firstSizeByte = firstChar;
    }
    
    public void handleRead() throws IOException {
        if (size == 0 && !readSize()) 
            return; // try next time.
        if (!buf.hasRemaining()) 
            throw new IllegalStateException("Token is done - don't read to it");
        
        int read = 0;
        while(buf.hasRemaining() && (read = chan.read(buf)) > 0);
        
        if (read == -1 && buf.hasRemaining())
            throw new IOException("closed before end of String token");
        
        if (!buf.hasRemaining())
            result = new String((byte[]) result, "ISO-8859-1");
    }
    
    private boolean readSize() throws IOException {
        if (sizeToken == null) 
            sizeToken = new BELong(chan,COLON,firstSizeByte);
        
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
            else
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
