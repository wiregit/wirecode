package com.limegroup.gnutella.connection;



import com.limegroup.gnutella.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;

public class InflaterReader implements ChannelReader, ReadableByteChannel {
    
    private Inflater inflater;
    private ReadableByteChannel channel;
    private ByteBuffer buf;
    
    public InflaterReader(ReadableByteChannel channel, Inflater inflater ) {
        if(channel == null)
            throw new NullPointerException("null channel!");
        
        this.channel = channel;
        this.inflater = inflater;
        this.buf = ByteBuffer.allocate(512);
    }
    
    /**
     * Sets the new channel.
     */
    public void setReadChannel(ReadableByteChannel channel) {
        if(channel == null)
            throw new NullPointerException("cannot set null channel!");

        this.channel = channel;
    }
    
    /**
     * Reads from this' inflater into the given ByteBuffer.
     */
    public int read(ByteBuffer buffer) throws IOException {
        int written = 0;
        int read = 0;
        
        // first try to inflate any prior input from the inflater.
        if(buffer.hasRemaining())
            written += inflate(buffer);
            
        // if we couldn't inflate anything, add input to the inflater
        // & inflate it.
        if(written == 0) {
            // First gobble up any data from the channel we're dependent on.
            while(buffer.hasRemaining() && (read = channel.read(buf)) > 0);
            
            // Then put that data into the inflater.
            inflater.setInput(buf.array(), 0, buf.position());
            buf.clear();
            
            written += inflate(buffer);
        }
        
        if(written > 0)
            return written;
        else if(read == -1)
            return read;
        else
            return 0;
    }
    
    /** Inflates data to this buffer. */
    private int inflate(ByteBuffer buffer) throws IOException {
        int written = 0;
        
        // Then, while there's room in the output buffer & there's inflatable data,
        // write to it from the inflater
        while(buffer.hasRemaining()) {
            int inflated;
            int position = buffer.position();
            try {
                inflated = inflater.inflate(buffer.array(), position, buffer.remaining());
            } catch(DataFormatException dfe) {
                IOException x = new IOException();
                x.initCause(dfe);
                throw x;
            } catch(NullPointerException npe) {
                // possible if the inflater was closed on a separate thread.
                IOException x = new IOException();
                x.initCause(npe);
                throw x;
            }
            
            if(inflated == 0)
                break;
                
            written += inflated;
            buffer.position(position + inflated);
        }
        
        return written;
    }
    
    /**
     * Determines if this reader is open.
     */
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    /**
     * Closes this channel.
     */
    public void close() throws IOException {
        channel.close();
    }
}
    
    