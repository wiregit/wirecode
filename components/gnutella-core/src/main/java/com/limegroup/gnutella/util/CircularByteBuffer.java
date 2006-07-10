package com.limegroup.gnutella.util;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.limegroup.gnutella.ErrorService;

/**
 * A circular buffer - allows to read and write to and from channels and other buffers
 * with virtually no memory overhead.
 */
public class CircularByteBuffer {

	private static final DevNull DEV_NULL = new DevNull();
	
    private final ByteBuffer in, out;
    
    private boolean lastOut = true;
    
    public CircularByteBuffer(int capacity, boolean direct) {
        if (direct) 
            in = ByteBuffer.allocateDirect(capacity);
        else 
            in = ByteBuffer.allocate(capacity);
        
        out = in.asReadOnlyBuffer();
    }
    
    public final int remainingIn() {
        int i = in.position();
        int o = out.position();
        if (i > o)
            return in.capacity() - i + o;
        else if (i < o)
            return o - i;
        else
        	return lastOut ? in.capacity() : 0;
    }
    
    public final int remainingOut() {
        return capacity() - remainingIn();
    }

    public void put(ByteBuffer src) {
        if (src.remaining() > remainingIn())
            throw new BufferOverflowException();
        
        if (src.hasRemaining())
        	lastOut = false;
        else 
        	return;
        
        if (src.remaining() > in.remaining()) {
            int oldLimit = src.limit();
            src.limit(src.position() + in.remaining());
            in.put(src);
            in.rewind();
            src.limit(oldLimit);
        }
        
        in.put(src);
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
        byte ret = out.get();
        allignIfEmpty();
        lastOut = true;
        return ret;
    }
    
    public void get(byte [] dest) {
        get(dest,0,dest.length);
    }
    
    public void get(byte [] dest, int offset, int length) {
        if (remainingOut() < length)
            throw new BufferUnderflowException();
        
        if (length > 0)
        	lastOut = true;
        else
        	return;
        
        if (out.remaining() < length) {
            int remaining = out.remaining();
            out.get(dest, offset, remaining);
            offset+=remaining;
            length-=remaining;
            out.rewind();
        }
        
        out.get(dest,offset,length);
        allignIfEmpty();
    }
    
    public void get(ByteBuffer dest) {
        if (remainingOut() < dest.remaining())
            throw new BufferUnderflowException();
        
        if (dest.remaining() > 0)
        	lastOut = true;
        else
        	return;
        
        if (out.remaining() < dest.remaining()) { 
            dest.put(out);
            out.rewind();
        }
        out.limit(out.position() + dest.remaining());
        dest.put(out);
        out.limit(out.capacity());
        allignIfEmpty();
    }
    
    private void allignIfEmpty() {
    	if (out.position() == in.position()) {
    		out.position(0);
    		in.position(0);
    	}
    }
    
    public int write(WritableByteChannel sink, int len) throws IOException {
        int written = 0;
        int thisTime = 0;
        while (remainingOut() > 0 && written < len) {
            if (!out.hasRemaining())
                out.rewind();
            if (in.position() > out.position()) {
            	if (len < 0)
            		out.limit(in.position());
            	else
            		out.limit(Math.min(in.position(), len - written + out.position()));
            }
            try {
            	thisTime = sink.write(out);
            } finally {
            	if (thisTime > 0)
            		lastOut = true;
            }
            
            out.limit(out.capacity());
            if (thisTime == 0)
                break;
            
            written += thisTime;
        }
        allignIfEmpty();
        return written;
    }
    
    public int write(WritableByteChannel sink) throws IOException {
    	return write(sink, -1);
    }
    
    public int read(ReadableByteChannel source) throws IOException {
        int read = 0;
        int thisTime = 0;
        
        while (remainingIn() > 0){
            if (!in.hasRemaining())
                in.rewind();
            if (out.position() > in.position()) 
                in.limit(out.position());
            try {
            	thisTime = source.read(in);
            } finally {
            	if (thisTime > 0)
            		lastOut = false;
            }
            
            in.limit(in.capacity());
            if (thisTime == 0)
                break;
            if (thisTime == -1)
            	throw new IOException();
            
            read += thisTime;
        } 
        
        return read;
    }
    
    public int size() {
    	return remainingOut();
    }
    
    public int capacity() {
    	return in.capacity();
    }
    
    public String toString() {
    	return "circular buffer in:"+in+" out:"+out;
    }
    
    public void order(ByteOrder order) {
    	out.order(order);
    }
    
    public int getInt() throws BufferUnderflowException {
    	if (remainingOut() < 4) 
    		throw new BufferUnderflowException();
    	
    	if (out.order() == ByteOrder.BIG_ENDIAN)
    		return getU() << 24 | getU() << 16 | getU() << 8 | getU();
    	else
    		return getU() | getU() << 8 | getU() << 16 | getU() << 24;
    }
    
    private int getU() {
    	return get() & 0xFF;
    }
    public void discard(int num) {
    	if (remainingOut() < num)
    		throw new BufferUnderflowException();
    	try {
    		write(DEV_NULL, num);
    	} catch (IOException impossible){
    		ErrorService.error(impossible);
    	}
    }
    
    private static class DevNull implements WritableByteChannel {

		public int write(ByteBuffer src) throws IOException {
			return 0;
		}

		public void close() throws IOException {}

		public boolean isOpen() {
			return false;
		}
    	
    }
}
