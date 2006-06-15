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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Like ByteArrayInputStream but for ByteBuffer(s) and this Class is not
 * synchronized!
 */
public class ByteBufferInputStream extends InputStream {

    protected final ByteBuffer buffer;

    public ByteBufferInputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }
    
    public int available() throws IOException {
        return buffer.remaining();
    }

    /** Does nothing */
    public void close() throws IOException {
    }

    public void mark(int readlimit) {
        buffer.mark();
    }

    public void reset() throws IOException {
        buffer.reset();
    }

    public boolean markSupported() {
        return true;
    }

    public int read() throws IOException {
        if (available() == 0) {
            return -1;
        }

        return buffer.get() & 0xFF;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0)
                || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }

        int available = available();
        if (available == 0) {
            return -1;
        } else if (len > available) {
            len = available;
        }

        buffer.get(b, off, len);
        return len;
    }

    public long skip(long n) throws IOException {
        long newPos = ((long) buffer.position() & 0xFFFFFFFFL) + n;
        long limit = (long) buffer.limit() & 0xFFFFFFFFL;
        if (newPos > limit) {
            newPos = limit;
        }

        if (n < 0L || newPos < 0L) {
            return 0L;
        }
        
        buffer.position((int) newPos);
        return n;
    }
}
