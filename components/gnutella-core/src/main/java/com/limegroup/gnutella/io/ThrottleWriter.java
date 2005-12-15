
// Edited for the Learning branch

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
 * 
 * 
 * Extends and Implements
 * ChannelWriter:        This object has a sink channel it writes to, setWriteChannel() and getWriteChannel().
 * InterestWriteChannel: The object that gives us data can tell us it has some, interest().
 * WriteObserver:        NIO can tell this object to get data and write now, handleWrite().
 * ThrottleListener:     (do)
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
        throttle.interest(this);
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
                throttle.interest(this);
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
            
        if(available == 0)
            return 0;

        int priorLimit = buffer.limit();
        if(buffer.remaining() > available)
            buffer.limit(buffer.position() + available);
            
        int totalWrote = channel.write(buffer);
        
        available -= totalWrote;
        buffer.limit(priorLimit);
        
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
            
        available = throttle.request();
        // If nothing is available, DO NOT CHANGE INTEREST WITHOUT
        // TRYING TO WRITE.  Otherwise, because of a bug(?) in selecting,
        // we will not be immediately notified again that data can be
        // written.  If we leave the interest alone then we will be
        // notified again.
        if(available != 0) {
            try {
                chain.interest(this, false);
                if(interested != null)
                    interested.handleWrite();
            } finally {
                throttle.release(available);
            }
            interested = observer; // re-get it, since observer may have changed interest.
            if(interested != null) {
                throttle.interest(this);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
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