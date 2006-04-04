package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;

/**
 * A collection of useful ByteBuffer utilities.
 */
public class BufferUtils {
    
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    
    /** Retrieves an empty ByteBuffer. */
    public static ByteBuffer getEmptyBuffer() {
        return EMPTY_BUFFER;
    }
    
    public static int transfer(ByteBuffer from, ByteBuffer to) {
        if(from == null)
            return 0;
        
        int read = 0;

        if(from.position() > 0) {
            from.flip();
            int remaining = from.remaining();
            int toRemaining = to.remaining();
            if(toRemaining >= remaining) {
                to.put(from);
                read += remaining;
            } else {
                int limit = from.limit();
                int position = from.position();
                from.limit(position + toRemaining);
                to.put(from);
                read += toRemaining;
                from.limit(limit);
            }
            from.compact();
        }
        
        return read;
    }

    /**
     * Transfers as much data as possible from from to to.
     * Returns how much data was transferred.
     * 
     * @param from
     * @param to
     * @return
     */
    public static int transfer(ByteBuffer from, ByteBuffer to, boolean needsFlip) {
        if(needsFlip)
            return transfer(from, to);
        else {
        
        if(from == null)
            return 0;
        
        int read = 0;

        if(from.hasRemaining()) {
            int remaining = from.remaining();
            int toRemaining = to.remaining();
            if(toRemaining >= remaining) {
                to.put(from);
                read += remaining;
            } else {
                int limit = from.limit();
                int position = from.position();
                from.limit(position + toRemaining);
                to.put(from);
                read += toRemaining;
                from.limit(limit);
            }
        }
        
        return read;
        }
    }
    
    /**
     * Reads data from the ByteBuffer, inserting it into the StringBuffer,
     * until a full line is read.  Returns true if a full line is read, false
     * if more data needs to be inserted into the buffer until a full line
     * can be read.
     */
    public static boolean readLine(ByteBuffer buffer, StringBuffer sBuffer) {
        int c = -1; //the character just read        
        while(buffer.hasRemaining()) {
            c = buffer.get();
            switch(c) {
                // if this was a \n character, we're done.
                case  '\n': return true;
                // if this was a \r character, ignore it.
                case  '\r': continue;                        
                // if it was any other character, append it to the buffer.
                default: sBuffer.append((char)c);
            }
        }

        return false;
    }
}
