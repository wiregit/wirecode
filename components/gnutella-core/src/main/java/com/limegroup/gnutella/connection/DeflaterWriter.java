package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.zip.*;

/**
 * A channel that deflates data written to it & writes the deflated
 * data to another sink.
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
    
    /**
     * Constructs a new DeflaterWriter with the given deflater.
     * You MUST call setWriteChannel prior to handleWrite.
     */
    public DeflaterWriter(Deflater deflater) {
        this(deflater, null);
    }
    
    /**
     * Constructs a new DeflaterWriter with the given deflater & channel.
     */
    public DeflaterWriter(Deflater deflater, InterestWriteChannel channel) {
        this.deflater = deflater;
        this.incoming = ByteBuffer.allocate(4 * 1024);
        this.outgoing = ByteBuffer.allocate(512);
        outgoing.flip();
        this.channel = channel;
    }
    
    /** Retreives the sink. */
    public InterestWriteChannel getWriteChannel() {
        return channel;
    }
    
    /** Sets the sink. */
    public void setWriteChannel(InterestWriteChannel channel) {
        this.channel = channel;
        channel.interest(this, true);
    }
    
    /**
     * Used by an observer to interest themselves in when something can
     * write to this.
     *
     * We must synchronize interest setting so that in the writing loop
     * we can ensure that interest isn't turned on between the time we
     * get the interested party, check for null, and turn off interest
     * (if it was null).
     */
    public synchronized void interest(WriteObserver observer, boolean status) {
        this.observer = status ? observer : null;
        InterestWriteChannel source = channel;
        // just always set interest on.  it's easiest & it'll be turned off
        // immediately once we're notified if we don't wanna do anything.
        // note that if we did want to do it correctly, we'd have to check
        // incoming.hasRemaining() || outgoing.hasRemaining(), but since
        // interest can be called in any thread, we'd have to introduce
        // locking around incoming & outgoing, which just isn't worth it.
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
     * Writes as much data as possible to the underlying source.
     * This tries to write any previously unwritten data, then tries
     * to deflate any new data, then tries to get more data by telling
     * its interested-observer to write to it.  This continues until
     * there is no more data to be written or the sink is full.
     */
    public boolean handleWrite() throws IOException {
        InterestWriteChannel source = channel;
        if(source == null)
            throw new IllegalStateException("writing with no source.");
            
        while(true) {
            // Step 1: See if there is any pending deflated data to be written.
            while(outgoing.hasRemaining() && source.write(outgoing) > 0);
            if(outgoing.hasRemaining())
                return true; // there is still deflated data that is pending a write.

            while(true) {
                // Step 2: Try and deflate the existing data.
                int deflated = deflater.deflate(outgoing.array());
                if(deflated > 0) {
                    outgoing.position(0).limit(deflated);
                    break; // we managed to deflate some data, try to write it...
                }
                    
                // Step 3: Normal deflate didn't work, try to simulate a Z_SYNC_FLUSH
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
                    if(incoming.position() == 0){
                        // We have nothing left to write, however, it is possible
                        // that between the above check for interested.handleWrite & here,
                        // we got pre-empted and another thread turned on interest.
                        synchronized(this) {
                            if(observer == null) // no observer? good, we can turn interest off
                                source.interest(this, false);
                            // else, we've got nothing to write, but our observer might.
                        }
                        return false;
                    }
                }
                
                //Step 5: We've got new data to deflate.
                deflater.setInput(incoming.array(), 0, incoming.position());
                incoming.clear();
                sync = 0;
            }
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