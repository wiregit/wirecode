package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;

public class InflaterReader implements ChannelReader, ReadableByteChannel {
    
    /** the inflater that will do the decompressing for us */
    private Inflater inflater;
    
    /** the channel this reads from */
    private ReadableByteChannel channel;
    
    /** the temporary buffer that data from the channel goes to prior to inflating */
    private ByteBuffer data;
    
    public InflaterReader(ReadableByteChannel channel, Inflater inflater ) {
        if(channel == null)
            throw new NullPointerException("null channel!");
        
        this.channel = channel;
        this.inflater = inflater;
        this.data = ByteBuffer.allocate(512);
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
        
        // inflate loop... inflate -> read -> rinse -> lather -> repeat as necessary.
        // only break out of this loop if 
        // a) output buffer gets full
        // b) inflater finishes or needs a dictionary
        // c) no data can be inflated & no data can be read off the channel
        while(buffer.hasRemaining()) { // (case a above)
            // first try to inflate any prior input from the inflater.
            int inflated = inflate(buffer);
            written += inflated;
            
            // if we couldn't inflate anything...
            if(inflated == 0) {
                // if this inflater is done or needs a dictionary, we're screwed. (case b above)
        		if (inflater.finished() || inflater.needsDictionary()) {
                    read = -1;
                    break;
        		}
            
                // if the buffer needs input, add it.
                if(inflater.needsInput()) {
                    // First gobble up any data from the channel we're dependent on.
                    while(data.hasRemaining() && (read = channel.read(data)) > 0);
                    // if we couldn't read any data, we suck. (case c above)
                    if(data.position() == 0)
                        break;
                    
                    // Then put that data into the inflater.
                    inflater.setInput(data.array(), 0, data.position());
                    data.clear();
                }
            }
            
            // if we're here, we either:
            // a) inflated some data
            // b) didn't inflate, but read some data that we input'd to the inflater
            
            // if a), we'll continue trying to inflate so long as the output buffer
            // has space left.
            // if b), we try to inflate and ultimately end up at a).
        }
        
        if(written > 0)
            return written;
        else if(read == -1)
            return -1;
        else
            return 0;
    }
    
    /** Inflates data to this buffer. */
    private int inflate(ByteBuffer buffer) throws IOException {
        int written = 0;
        
        int position = buffer.position();
        try {
            written = inflater.inflate(buffer.array(), position, buffer.remaining());
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
            
        buffer.position(position + written);
        
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
    
    