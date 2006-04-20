package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A collection of useful ByteBuffer utilities.
 */
public class BufferUtils {
    
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    
    /** Retrieves an empty ByteBuffer. */
    public static ByteBuffer getEmptyBuffer() {
        return EMPTY_BUFFER;
    }
    
    /**
     * Cleans some data from the buffer.
     * Returns how much more needs to be deleted.
     */
    public static long delete(ByteBuffer buffer, long amountToDelete) {
        if (buffer.position() <= amountToDelete) {
            amountToDelete -= buffer.position();
            buffer.clear();
        } else {
            int keep = (int) amountToDelete - buffer.position();
            // assume keep is 3, we want to ditch ABCD but keep EFG
            // now: [ABCDEFG* ] where * is position, ] is limit and capacity
            buffer.flip();
            // now: [*BCDEFG^ ] where * is position, ^ is limit, ] is capacity
            buffer.position(buffer.limit() - keep);
            // now: [ABCD*FG^ ] where * is position, ^ is limit, ] is capacity
            buffer.compact();
            // now: [EFG* ] where * is position, ] is limit and capacity

            amountToDelete = 0;
        }
        return amountToDelete;
    }
    
    /**
     * Transfers all data from 'temporary' to 'dest', and then reads as much data
     * as possible from 'channel' into 'dest'.
     * This returns the last amount of data that could be read from the channel.
     * It does NOT return the total amount of data transferred.
     * 
     * @param channel
     * @param dest
     * @param temporary
     * @return The last amount of data that could be read from the channel.
     * @throws IOException
     */
    public static int readAll(ReadableByteChannel channel, ByteBuffer dest, ByteBuffer temporary) throws IOException {
        transfer(temporary, dest);
        int read = 0;
        while(dest.hasRemaining() && (read = channel.read(dest)) > 0);
        return read;
    }
    
    /**
     * Transfers as much data as possible from from to to.
     * The data in 'to' will be flipped prior to transferring & then compacted.
     * Returns however much data was transferred.
     */
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
     * The data in 'to' will NOT be flipped prior to transferring.
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
