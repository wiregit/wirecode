/*
 * Mojito Distributed Hash Tabe (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Like ByteArrayInputStream but for ByteBuffer(s) and this Class is not
 * synchronized!
 */
public class ByteBufferInputStream extends InputStream {

    /** An empty ByteBuffer */
    private static final ByteBuffer[] EMPTY = new ByteBuffer[0];

    /** The index of the current ByteBuffer */
    protected int index = 0;
    
    /** The index of the marked ByteBuffer */
    protected int mark = -1;
    
    /** The array of ByteBuffer(s) we're reading from */
    protected ByteBuffer[] buffers;
    
    /**
     * Creates a ByteBufferInputStream that is reading from
     * one or more ByteBuffer(s).
     */
    public ByteBufferInputStream(ByteBuffer... buffers) {
        this.buffers = buffers;
    }
    
    /**
     * Returns the underlying array of ByteBuffer(s).
     */
    public ByteBuffer[] buffers() {
        if (buffers == EMPTY) {
            throw new IllegalStateException("InputStream is closed");
        }
        
        return buffers;
    }
    
    public int available() throws IOException {
        int available = 0;
        for(int i = buffers.length-1; i >= index; --i) {
            available += buffers[i].remaining();
        }
        return available;
    }
    
    public void close() throws IOException {
        index = 0;
        mark = -1;
        buffers = EMPTY;
    }

    public void mark(int readlimit) {
        if (index < buffers.length) {
            mark = index;
            for(int i = buffers.length-1; i >= mark; --i) {
                buffers[i].mark();
            }
        }
    }

    public void reset() throws IOException {
        if (mark != -1) {
            index = mark;
            for(int i = buffers.length-1; i >= index ; --i) {
                buffers[i].reset();
            }
            mark = -1;
        }
    }

    public boolean markSupported() {
        return true;
    }

    public int read() throws IOException {
        while(index < buffers.length) {
            ByteBuffer b = buffers[index];
            if (b.hasRemaining()) {
                return b.get() & 0xFF;
            }
            
            // Try next ByteBuffer
            ++index;
        }
        
        return -1;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0)
                || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        
        if (index >= buffers.length) {
            return -1;
        } else if (index == buffers.length-1 
                && !buffers[index].hasRemaining()) {
            return -1;
        } else if (len == 0 || b.length == 0) {
            return (available() > 0 ? 0 : -1);
        }
        
        int r = 0;
        while(index < buffers.length) {
            ByteBuffer buf = buffers[index];
            
            // The number of bytes we can read
            int l = Math.min(buf.remaining(), len-r);
            
            // Do read
            buf.get(b, off+r, l);
            r += l;
            
            // Exit if done
            if (r == len) {
                break;
            }
            
            // Try next ByteBuffer
            ++index;
        }
        
        // Return EOF if no bytes were available
        return (r > 0 ? r : -1);
    }
    
    public long skip(long n) throws IOException {
        long s = 0L;
        while(index < buffers.length) {
            ByteBuffer b = buffers[index];
            
            // The number of bytes we can skip in this ByteBuffer
            long l = Math.min((long)b.remaining(), n-s);
            b.position(b.position() + (int)l);
            s += l;
            
            // Exit if done
            if (s == n) {
                break;
            }
            
            // Try next ByteBuffer
            ++index;
        }
        
        return s;
    }
}
