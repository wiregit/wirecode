package com.limegroup.gnutella.io;

import java.nio.ByteBuffer;

/**
 * A collection of useful ByteBuffer utilities.
 */
public class BufferUtils {

    /**
     * Transfers as much data as possible from from to to.
     * Returns how much data was transferred.
     * 
     * @param from
     * @param to
     * @return
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
}
