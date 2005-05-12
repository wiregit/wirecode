package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

/**
 * A writer that throttles data according to a throttle.
 */
public class ThrottleWriter implements ChannelWriter, InterestWriteChannel, ThrottleListener {
    
    /** The channel to write to & interest on. */    
    private volatile InterestWriteChannel channel;
    /** The last observer. */
    private volatile WriteObserver observer;
    /** The throttle we're using. */
    private final Throttle throttle;
    /** The amount of data we were told we can write. */
    private int available;    
    /** The object that the Throttle will recognize as the SelectionKey attachments */
    private Object attachment;
    /** Whether or not the last call to write(ByteBuffer) was able to write everything. */
    private boolean wroteAll;
    
    /**
     * Constructs a ThrottleWriter with the given Throttle.
     *
     * You MUST call setWriteChannel prior to using this.
     */
    public ThrottleWriter(Throttle throttle) {
        this(throttle, null);
    }
    
    /**
     * Constructs a new ThrottleWriter with the given throttle & channel.
     */
    public ThrottleWriter(Throttle throttle, InterestWriteChannel channel) {
        this.throttle = throttle;
        this.channel = channel;
    }
    
    /** Retreives the sink. */
    public InterestWriteChannel getWriteChannel() {
        return channel;
    }
    
    /** Sets the sink. */
    public void setWriteChannel(InterestWriteChannel channel) {
        this.channel = channel;
        throttle.interest(this, attachment);
    }
    
    /** Sets the attachment that the Throttle will recognize for this Writer. */
    public void setAttachment(Object att) {
        attachment = att;
    }
    
    /** Gets the attachment. */
    public Object getAttachment() {
        return attachment;
    }
    
    /**
     * Tells the Throttle that we're interested in receiving bandwidthAvailable
     * events at some point in time.
     */
    public void interest(WriteObserver observer, boolean status) {
        if(status) {
            this.observer = observer;
            throttle.interest(this, attachment);
        } else {
            this.observer = null;
        }
    }
    
    /**
     * Notification from the Throttle that bandwidth is available.
     * Returns false if this no longer is open & will not be interested
     * ever again.
     */
    public boolean bandwidthAvailable() {
        if(channel.isOpen()) {
            channel.interest(this, true);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Writes data to the chain.
     */
    public int write(ByteBuffer buffer) throws IOException {
        InterestWriteChannel chain = channel;
        if(chain == null)
            throw new IllegalStateException("writing with no chain!");

        int priorLimit = buffer.limit();
        if(buffer.remaining() > available) {
            System.out.println("had more data than can write: " + buffer.remaining() + ", av: " + available);
            buffer.limit(buffer.position() + available);
        }
            
        int wrote = 0;
        int totalWrote = 0;
        
        System.out.println("remaining: " + buffer.remaining());
        while(buffer.hasRemaining() && (wrote = channel.write(buffer)) > 0) {
            System.out.println("wrote: " + wrote);
            totalWrote += wrote;
        }
        System.out.println("now remaining " + buffer.remaining());
                    
        available -= totalWrote;
        buffer.limit(priorLimit);

        // we wrote everything we could to this if the buffer is empty
        // or if the channel wouldn't accept any more data.
        wroteAll = !buffer.hasRemaining() || wrote == 0;

        System.out.println("av: " + available + ", buffer: " + buffer);
        
        return totalWrote;
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
     * Requests some space from the Throttle to write data.
     *
     * A global 'available' variable is set, and it is assumed that
     * the interested party will try writing to us.  Our chained-write
     * is limited to the available amount, and available is decremented.
     * We then release the amount of space that we couldn't write.
     */
    public boolean handleWrite() throws IOException {
        InterestWriteChannel chain = channel;
        if(chain == null)
            throw new IllegalStateException("writing with no source.");
            
        WriteObserver interested = observer;
            
        available = throttle.request(this, attachment);
        wroteAll = true;
        try {
            chain.interest(this, false);
            if(available > 0 && interested != null)
                interested.handleWrite();
        } finally {
            throttle.release(available, wroteAll, this, attachment);
        }
        
        interested = observer; // re-get it, since observer may have changed interest.
        if(interested != null) {
            throttle.interest(this, attachment);
            return true;
        } else {
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