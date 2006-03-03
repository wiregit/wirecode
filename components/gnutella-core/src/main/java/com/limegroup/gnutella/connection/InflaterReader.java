package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

import com.limegroup.gnutella.io.ChannelReader;
import com.limegroup.gnutella.io.InterestReadChannel;

/**
 * Reads data from a source channel and offers the inflated version for reading.
 *
 * Each invocation of read(ByteBuffer) will attempt to return any inflated data.
 * If no data is available for inflation, data will be read from the source channel
 * and inflation will be attempted.  The ByteBuffer will be filled as much as possible
 * without blocking.
 *
 * The source channel may not be entirely emptied out in a single call to read(ByteBuffer),
 * because the supplied ByteBuffer may not be large enough to accept all inflated data.
 * If this is the case, the data will remain in the source channel until further calls to
 * read(ByteBuffer).
 *
 * The source channel does not need to be set for construction.  However, before read(ByteBuffer)
 * is called, setReadChannel(ReadableByteChannel) must be called with a valid channel.
 */
public class InflaterReader implements ChannelReader, InterestReadChannel {
    
    /** the inflater that will do the decompressing for us */
    private Inflater inflater;
    
    /** the channel this reads from */
    private InterestReadChannel channel;
    
    /** the temporary buffer that data from the channel goes to prior to inflating */
    private ByteBuffer data;
    
    /**
     * Constructs a new InflaterReader without an underlying source.
     * Prior to read(ByteBuffer) being called, setReadChannel(ReadableByteChannel)
     * MUST be called.
     */
    public InflaterReader(Inflater inflater) {
        this(null, inflater);
    }
    
    /**
     * Constructs a new InflaterReader with the given source channel & inflater.
     */
    public InflaterReader(InterestReadChannel channel, Inflater inflater ) {        
        if(inflater == null)
            throw new NullPointerException("null inflater!");

        this.channel = channel;
        this.inflater = inflater;
        this.data = ByteBuffer.allocate(512);
    }
    
    /**
     * Sets the new channel.
     */
    public void setReadChannel(InterestReadChannel channel) {
        if(channel == null)
            throw new NullPointerException("cannot set null channel!");

        this.channel = channel;
    }
    
    /** Gets the read channel */
    public InterestReadChannel getReadChannel() {
        return channel;
    }
    
    public void interest(boolean status) {
        channel.interest(status);
    }
    
    /**
     * Reads from this' inflater into the given ByteBuffer.
     */
    public int read(ByteBuffer buffer) throws IOException {
        int written = 0;
        int read = 0;
        
        // inflate loop... inflate -> read -> lather -> rinse -> repeat as necessary.
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
    
    