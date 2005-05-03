package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.zip.*;

/**
 * Buffers data until it reaches a certain size (or enough time passes that
 * we definitely want to deflate).
 */
public class DeflaterWriter implements ChannelWriter, InterestWriteChannel {
    
    /** The channel to write to & interest on. */    
    private volatile InterestWriteChannel channel;
    /** The last observer. */
    private volatile WriteObserver observer;
    /** The buffer used for deflating into. */
    private ByteBuffer outgoing;
    /** The buffer used for writing data into. */
    private ByteBuffer incoming;
    /** The deflater to use */
    private Deflater deflater;
    /** The sync level we're on.  0: not sync, 1: NO_COMPRESSION, 2: DEFAULT */
    private int sync = 0;
    /** An empty byte array to reuse. */
    private static final byte[] EMPTY = new byte[0];
    
    public DeflaterWriter(Deflater deflater) {
        this(deflater, null);
    }
    
    public DeflaterWriter(Deflater deflater, InterestWriteChannel channel) {
        this.deflater = deflater;
        this.incoming = ByteBuffer.allocate(4 * 1024);
        this.outgoing = ByteBuffer.allocate(512);
        outgoing.flip();
        this.channel = channel;
    }
    
    public InterestWriteChannel getWriteChannel() {
        return channel;
    }
    
    public void setWriteChannel(InterestWriteChannel channel) {
        this.channel = channel;
        channel.interest(this, true);
    }
    
    public void interest(WriteObserver observer, boolean status) {
        this.observer = status ? observer : null;
        InterestWriteChannel source = channel;
        if(source != null)
            source.interest(this, true);
    }
    
    /**
     * Writes data to our internal buffer, if there's room.
     */
    public int write(ByteBuffer buffer) throws IOException {
        int wrote = 0;
        
        if(incoming.hasRemaining()) {
            int remaining = incoming.remaining();
            int adding = buffer.remaining();
            if(remaining >= adding) {
                incoming.put(buffer);
                wrote = adding;
            } else {
                int oldLimit = buffer.limit();
                int position = buffer.position();
                buffer.limit(position + remaining);
                incoming.put(buffer);
                buffer.limit(oldLimit);
                wrote = remaining;
            }
        }
        
        return wrote;
    }
    
    /** Closes the underlying channel. */
    public void close() throws IOException {
        Channel source = channel;
        if(source != null)
            source.close();
    }
    
    /** Determines if the underlying channel is open. */
    public boolean isOpen() {
        Channel source = channel;
        return source != null ? source.isOpen() : false;
    }
    
    /**
     * Attempts to write deflated data to the underlying source.
     *      
     */
    public boolean handleWrite() throws IOException {
        InterestWriteChannel source = channel;
        if(source == null)
            throw new IllegalStateException("writing with no source.");
            
        while(true) {
            // Step 1: See if there is any pending deflated data to be written.
            while(outgoing.hasRemaining() && source.write(outgoing) > 0);
            if(outgoing.hasRemaining())
                break; // there is still deflated data that is pending a write.
            
            // Step 2: Try and deflate the existing data.
            int deflated = deflater.deflate(outgoing.array());
            if(deflated > 0) {
                outgoing.position(0).limit(deflated);
                continue; // we managed to deflate some data!
            }
                
            // Step 3: Normal deflate didn't work, try to simulate a SYNCH_FLUSH
            // Note that this requires we tried deflating until deflate returned 0
            // above.  Otherwise, this setInput call would erase prior input.
            // We must use different levels of syncing because we have to make sure
            // that we write everything out of deflate after each level is set.
            // Otherwise compression doesn't work.
            if(sync == 0) {
                deflater.setInput(EMPTY);
                deflater.setLevel(Deflater.NO_COMPRESSION);
                sync = 1;
                continue;
            } else if(sync == 1) {
                deflater.setLevel(Deflater.DEFAULT_COMPRESSION);
                sync = 2;
                continue;
            }
            
            // Step 4: If we have no data, tell any interested parties to add some.
            if(incoming.position() == 0) {
                WriteObserver interested = observer;
                if(interested != null)
                    interested.handleWrite();
                
                // If still no data after that, we've written everything we want -- exit.
                if(incoming.position() == 0)
                    break;
            }
            
            //Step 5: We've got new data to deflate.
            deflater.setInput(incoming.array(), 0, incoming.position());
            incoming.clear();
            sync = 0;
        }
        
        if(outgoing.hasRemaining()) {
            return true;
        } else {
            source.interest(this, false); // we've written everything.
            return false;
        }
    }
    
    /** Shuts down the last observer. */
    public void shutdown() {
        Shutdownable listener = observer;
        if(listener != null)
            listener.shutdown();
    }
    
    /** Unused, Unsupported */
    public void handleIOException(IOException x) {
        throw new RuntimeException("Unsupported", x);
    }
}