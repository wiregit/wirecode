package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.Throttle;
import org.limewire.nio.ThrottleListener;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;

/**
 * A writer that throttles data according to a throttle.
 *
 * To work with the Throttle, this uses an attachment (which must be the same as the
 * attachment of the SelectionKey associated with the socket this is using).
 */
public class ThrottleWriter implements ChannelWriter, InterestWritableByteChannel, ThrottleListener {
    
    //private static final Log LOG = LogFactory.getLog(ThrottleWriter.class);
    
    /** The channel to write to & interest on. */    
    private volatile InterestWritableByteChannel channel;
    /** The last observer. */
    private volatile WriteObserver observer;
    /** The throttle we're using. */
    private volatile Throttle throttle;
    /** The amount of data we were told we can write. */
    private int available;    
    /** The object that the Throttle will recognize as the SelectionKey attachments */
    private Object attachment;
    /** Whether we interested the channel last time */
    private boolean channelInterested;
    
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
    public ThrottleWriter(Throttle throttle, InterestWritableByteChannel channel) {
        this.throttle = throttle;
        this.channel = channel;
    }
    
    /** Retreives the sink. */
    public InterestWritableByteChannel getWriteChannel() {
        return channel;
    }
    
    /** Sets the sink. */
    public void setWriteChannel(InterestWritableByteChannel channel) {
        this.channel = channel;
        Throttle t = this.throttle;
        if (t != null) {
            t.interest(this);
        } else if (channel != null) {
            channel.interestWrite(this, true);
        }
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
    public void interestWrite(WriteObserver observer, boolean status) {
        if(status) {
            this.observer = observer;
            if(channel != null) {
                Throttle t = this.throttle;
                if (t != null)
                    t.interest(this);
                else 
                    channel.interestWrite(this, status);
            }
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
        	if (!channelInterested) {
        		channelInterested = true;
        		channel.interestWrite(this, true);
        	}
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
        InterestWritableByteChannel chain = channel;
        if(chain == null)
            throw new IllegalStateException("writing with no chain!");
        
        // throttling is disabled, just forward to underlying channel
        if (throttle == null) {
            return chain.write(buffer);
        }
        
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
     * Requests some bandiwdth from the throttle.
     */
    public void requestBandwidth() {
        if (throttle != null) {
            available = throttle.request();
        }
    }
    
    /**
     * Releases available bandwidth back to the throttle.
     */
    public void releaseBandwidth() {
        if (throttle != null) {
            throttle.release(available);
            available = 0;
        }
    }
    
    /**
     * Writes up to 'available' data to the sink channel.
     * requestBandwidth must be called prior to this to allow data
     * to be written, and releaseBandwidth must be called afterwards
     * to return unwritten data back to the Throttle.
     */
    public boolean handleWrite() throws IOException {
        InterestWritableByteChannel chain = channel;
        if(chain == null)
            throw new IllegalStateException("writing with no source.");
        WriteObserver interested = observer;
        chain.interestWrite(this, false);
        channelInterested = false;
        if (available > 0 || throttle == null) {
        	if(interested != null)
        		interested.handleWrite();
        	interested = observer; // re-get it, since observer may have changed interest.
        }
        if(interested != null) {
            if (this.throttle != null)
                this.throttle.interest(this);
            else 
                chain.interestWrite(this, true);
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

    public boolean hasBufferedOutput() {
        InterestWritableByteChannel channel = this.channel;
        return channel != null && channel.hasBufferedOutput();
    }
 
    public void setThrottle(final Throttle throttle) {
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            public void run() {
                setThrottleInternal(throttle);
            }
        });        
    }

    protected void setThrottleInternal(Throttle throttle) {
        releaseBandwidth();
        
        this.throttle = throttle;
        
        if (throttle != null) {
            throttle.interest(ThrottleWriter.this);
        } else if (channel != null) {
            channel.interestWrite(this, true);
        }
    }
    
}