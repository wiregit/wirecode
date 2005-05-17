package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A writer that throttles data according to a throttle.
 *
 * To work with the Throttle, this uses an attachment (which must be the same as the
 * attachment of the SelectionKey associated with the socket this is using).
 */
public class ThrottleWriter implements ChannelWriter, InterestWriteChannel, ThrottleListener {
    
    private static final Log LOG = LogFactory.getLog(ThrottleWriter.class);
    
    
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
            if(channel != null)
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
     *
     * Only writes up to 'available' amount of data.
     */
    public int write(ByteBuffer buffer) throws IOException {
        InterestWriteChannel chain = channel;
        if(chain == null)
            throw new IllegalStateException("writing with no chain!");
            
        if(available == 0) {
            if(buffer.hasRemaining())
                wroteAll = false;
            return 0;
        }

        int priorLimit = buffer.limit();
        if(buffer.remaining() > available) {
            buffer.limit(buffer.position() + available);
        }
            
        int wrote = 0;
        int totalWrote = 0;
        
        while(buffer.hasRemaining() && (wrote = channel.write(buffer)) > 0)
            totalWrote += wrote;
                    
        available -= totalWrote;
        buffer.limit(priorLimit);

        // we wrote everything we could to this if the buffer is empty
        // or if the channel wouldn't accept any more data.
        wroteAll = !buffer.hasRemaining() || wrote == 0;
        
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
        wroteAll = available != 0;
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