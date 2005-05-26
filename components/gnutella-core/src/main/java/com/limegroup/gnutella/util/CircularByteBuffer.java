package com.limegroup.gnutella.util;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * A circular buffer - allows to read and write to and from channels and other buffers
 * with virtually no memory overhead.
 */
public class CircularByteBuffer {

    private final ByteBuffer in, out;
    private boolean lastOut = true;
    
    public CircularByteBuffer(int capacity, boolean direct) {
        if (direct) 
            in = ByteBuffer.allocateDirect(capacity);
        else 
            in = ByteBuffer.allocate(capacity);
        
        out = in.duplicate();
    }
    
    public final int remainingIn() {
        int i = in.position();
        int o = out.position();
        if (i > o)
            return in.capacity() - i + o;
        if (i < o)
            return o - i;
        else
            return lastOut ? in.capacity() : 0;
    }
    
    public final int remainingOut() {
        return in.capacity() - remainingIn();
    }

    public void put(ByteBuffer src) {
        if (src.remaining() > remainingIn())
            throw new BufferOverflowException();
        
        if (src.remaining() > in.remaining()) {
            int oldLimit = src.limit();
            src.limit(src.position() + in.remaining());
            in.put(src);
            in.rewind();
            src.limit(oldLimit);
        }
        
        in.put(src);
        lastOut = false;
    }
    
    public void put(CircularByteBuffer src) {
        if (src.remainingOut() > remainingIn())
            throw new BufferOverflowException();
        
        if (in.remaining() < src.remainingOut()) {
            src.out.limit(in.remaining());
            in.put(src.out);
            in.rewind();
            src.out.limit(src.out.capacity());
        }
        
        in.put(src.out);
        lastOut = false;
    }
    
    public byte get() {
        if (remainingOut() < 1)
            throw new BufferUnderflowException();
        if (!out.hasRemaining())
            out.rewind();
        lastOut = true;
        return out.get();
    }
    
    public void get(byte [] dest) {
        get(dest,0,dest.length);
    }
    
    public void get(byte [] dest, int offset, int length) {
        if (remainingOut() < length)
            throw new BufferUnderflowException();
        
        if (out.remaining() < length) {
            int remaining = out.remaining();
            out.get(dest, offset, remaining);
            offset+=remaining;
            length-=remaining;
            out.rewind();
        }
        
        out.get(dest,offset,length);
        lastOut = true;
    }
    
    public void get(ByteBuffer dest) {
        if (remainingOut() < dest.remaining())
            throw new BufferUnderflowException();
        
        if (out.remaining() < dest.remaining()) { 
            dest.put(out);
            out.rewind();
        }
        
        dest.put(out);
        lastOut = true;
    }
    
    public int write(WritableByteChannel sink) throws IOException {
        int written = 0;
        int thisTime = 0;
        while (remainingOut() > 0) {
            if (!out.hasRemaining())
                out.rewind();
            if (in.position() > out.position())
                out.limit(in.position());
            int pos = out.position();
            try {
                thisTime = sink.write(out);
            } finally {
                if (out.position() > pos)
                    lastOut = true;
            }
            
            out.limit(out.capacity());
            if (thisTime == 0)
                break;
            
            written += thisTime;
        }
        return written;
    }
    
    public int read(ReadableByteChannel source) throws IOException {
        int read = 0;
        int thisTime = 0;
        
        while (remainingIn() > 0){
            if (!in.hasRemaining())
                in.rewind();
            if (out.position() > in.position()) 
                in.limit(out.position());
            
            int pos = in.position();
            try {
                thisTime = source.read(in);
            } finally {
                if (in.position() > pos)
                    lastOut = false;
            }
            
            in.limit(in.capacity());
            if (thisTime == 0)
                break;
            
            read += thisTime;
        } 
        
        return read;
    }
}
