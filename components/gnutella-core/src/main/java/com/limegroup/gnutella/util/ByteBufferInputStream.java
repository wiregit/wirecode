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

    private int index = 0;
    private int mark = -1;
    
    private ByteBuffer[] buffers;
    
    public ByteBufferInputStream(ByteBuffer buffer) {
        this(new ByteBuffer[]{buffer});
    }
    
    public ByteBufferInputStream(ByteBuffer[] buffers) {
        this.buffers = buffers;
    }
    
    public int available() throws IOException {
        int available = 0;
        for(int i = buffers.length-1; i >= index; --i) {
            available += buffers[i].remaining();
        }
        return available;
    }
    
    /** Does nothing */
    public void close() throws IOException {
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
        }
        
        int r = 0;
        boolean zero = true;
        while(index < buffers.length) {
            ByteBuffer buf = buffers[index];
            
            // The number of bytes we can read
            int l = Math.min(buf.remaining(), len-r);
            
            // Remember if any bytes were available
            if (zero && buf.hasRemaining()) {
                zero = false;
            }
            
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
        return (zero ? -1 : r);
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
