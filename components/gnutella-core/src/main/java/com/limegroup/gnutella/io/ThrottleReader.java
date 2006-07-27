package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

/**
 * A reader that throttles data according to a throttle.
 *
 * To work with the Throttle, this uses an attachment (which must be the same as the
 * attachment of the SelectionKey associated with the socket this is using).
 */
public class ThrottleReader implements InterestReadChannel, ChannelReader, ThrottleListener {
    
    //private static final Log LOG = LogFactory.getLog(ThrottleReader.class);
    
    /** The channel to write to & interest on. */    
    private volatile InterestReadChannel channel;
    /** The throttle we're using. */
    private final Throttle throttle;
    /** The amount of data we were told we can read. */
    private int available;    
    /** The object that the Throttle will recognize as the SelectionKey attachments */
    private Object attachment;
    /** The last interest state, to interact well with the Throttle. */
    private volatile boolean lastInterestState;
    
    /**
     * Constructs a ThrottleReader with the given Throttle.
     *
     * You MUST call setWriteChannel prior to using this.
     */
    public ThrottleReader(Throttle throttle) {
        this(throttle, null);
    }
    
    /**
     * Constructs a new ThrottleWriter with the given throttle & channel.
     */
    public ThrottleReader(Throttle throttle, InterestReadChannel channel) {
        this.throttle = throttle;
        this.channel = channel;
    }
    
    /** Retreives the sink. */
    public InterestReadChannel getReadChannel() {
        return channel;
    }
    
    /** Sets the sink. */
    public void setReadChannel(InterestReadChannel channel) {
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
    public void interest(boolean status) {
        lastInterestState = status;
        if(channel != null) {
            if(status)
                throttle.interest(this);
            else
                channel.interest(false);
        }
    }
    
    /**
     * Notification from the Throttle that bandwidth is available.
     * Returns false if this no longer is open & will not be interested
     * ever again.
     */
    public boolean bandwidthAvailable() {
        if(channel.isOpen() && lastInterestState) {
            channel.interest(true);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Read data from the chain.
     *
     * Only reads up to 'available' amount of data.
     */
    public int read(ByteBuffer buffer) throws IOException {
        InterestReadChannel chain = channel;
        if(chain == null)
            throw new IllegalStateException("reading with no chain!");
            
        if(available == 0)
            return 0;

        int priorLimit = buffer.limit();
        if(buffer.remaining() > available) {
            //LOG.debug("Limting amount remaining to read from " + buffer.remaining() + " to " + available);
            buffer.limit(buffer.position() + available);
        }

        int totalRead = -1;
        try {
            totalRead = channel.read(buffer);
        } finally {
            buffer.limit(priorLimit);
        }
        
        if (totalRead > 0)
            available -= totalRead;
        //LOG.debug("Read: " + totalRead  + ", leaving: " + available + " left.");
        
        return totalRead;
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
     * Requests some bandwidth from the throttle.
     */
    public void requestBandwidth() {
        available = throttle.request();
        channel.interest(false);
    }
    
    /**
     * Releases available bandwidth back to the throttle.
     */
    public void releaseBandwidth() {
        throttle.release(available);
        available = 0;
        if(lastInterestState)
            throttle.interest(this);
    }
}
