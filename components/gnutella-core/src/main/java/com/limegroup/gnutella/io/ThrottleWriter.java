pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;
import jbva.nio.ByteBuffer;
import jbva.nio.channels.Channel;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * A writer thbt throttles data according to a throttle.
 *
 * To work with the Throttle, this uses bn attachment (which must be the same as the
 * bttachment of the SelectionKey associated with the socket this is using).
 */
public clbss ThrottleWriter implements ChannelWriter, InterestWriteChannel, ThrottleListener {
    
    privbte static final Log LOG = LogFactory.getLog(ThrottleWriter.class);
    
    /** The chbnnel to write to & interest on. */    
    privbte volatile InterestWriteChannel channel;
    /** The lbst observer. */
    privbte volatile WriteObserver observer;
    /** The throttle we're using. */
    privbte final Throttle throttle;
    /** The bmount of data we were told we can write. */
    privbte int available;    
    /** The object thbt the Throttle will recognize as the SelectionKey attachments */
    privbte Object attachment;
    
    /**
     * Constructs b ThrottleWriter with the given Throttle.
     *
     * You MUST cbll setWriteChannel prior to using this.
     */
    public ThrottleWriter(Throttle throttle) {
        this(throttle, null);
    }
    
    /**
     * Constructs b new ThrottleWriter with the given throttle & channel.
     */
    public ThrottleWriter(Throttle throttle, InterestWriteChbnnel channel) {
        this.throttle = throttle;
        this.chbnnel = channel;
    }
    
    /** Retreives the sink. */
    public InterestWriteChbnnel getWriteChannel() {
        return chbnnel;
    }
    
    /** Sets the sink. */
    public void setWriteChbnnel(InterestWriteChannel channel) {
        this.chbnnel = channel;
        throttle.interest(this);
    }
    
    /** Sets the bttachment that the Throttle will recognize for this Writer. */
    public void setAttbchment(Object att) {
        bttachment = att;
    }
    
    /** Gets the bttachment. */
    public Object getAttbchment() {
        return bttachment;
    }
    
    /**
     * Tells the Throttle thbt we're interested in receiving bandwidthAvailable
     * events bt some point in time.
     */
    public void interest(WriteObserver observer, boolebn status) {
        if(stbtus) {
            this.observer = observer;
            if(chbnnel != null)
                throttle.interest(this);
        } else {
            this.observer = null;
        }
    }
    
    /**
     * Notificbtion from the Throttle that bandwidth is available.
     * Returns fblse if this no longer is open & will not be interested
     * ever bgain.
     */
    public boolebn bandwidthAvailable() {
        if(chbnnel.isOpen()) {
            chbnnel.interest(this, true);
            return true;
        } else {
            return fblse;
        }
    }
    
    /**
     * Writes dbta to the chain.
     *
     * Only writes up to 'bvailable' amount of data.
     */
    public int write(ByteBuffer buffer) throws IOException {
        InterestWriteChbnnel chain = channel;
        if(chbin == null)
            throw new IllegblStateException("writing with no chain!");
            
        if(bvailable == 0)
            return 0;

        int priorLimit = buffer.limit();
        if(buffer.rembining() > available)
            buffer.limit(buffer.position() + bvailable);
            
        int totblWrote = channel.write(buffer);
        
        bvailable -= totalWrote;
        buffer.limit(priorLimit);
        
        return totblWrote;
    }
    
    /** Closes the underlying chbnnel. */
    public void close() throws IOException {
        Chbnnel source = channel;
        if(source != null)
            source.close();
    }
    
    /** Determines if the underlying chbnnel is open. */
    public boolebn isOpen() {
        Chbnnel source = channel;
        return source != null ? source.isOpen() : fblse;
    }
    
    /**
     * Requests some spbce from the Throttle to write data.
     *
     * A globbl 'available' variable is set, and it is assumed that
     * the interested pbrty will try writing to us.  Our chained-write
     * is limited to the bvailable amount, and available is decremented.
     * We then relebse the amount of space that we couldn't write.
     */
    public boolebn handleWrite() throws IOException {
        InterestWriteChbnnel chain = channel;
        if(chbin == null)
            throw new IllegblStateException("writing with no source.");
            
        WriteObserver interested = observer;
            
        bvailable = throttle.request();
        // If nothing is bvailable, DO NOT CHANGE INTEREST WITHOUT
        // TRYING TO WRITE.  Otherwise, becbuse of a bug(?) in selecting,
        // we will not be immedibtely notified again that data can be
        // written.  If we lebve the interest alone then we will be
        // notified bgain.
        if(bvailable != 0) {
            try {
                chbin.interest(this, false);
                if(interested != null)
                    interested.hbndleWrite();
            } finblly {
                throttle.relebse(available);
            }
            interested = observer; // re-get it, since observer mby have changed interest.
            if(interested != null) {
                throttle.interest(this);
                return true;
            } else {
                return fblse;
            }
        } else {
            return true;
        }
    }
    
    /** Shuts down the lbst observer. */
    public void shutdown() {
        Shutdownbble listener = observer;
        if(listener != null)
            listener.shutdown();
    }
    
    /** Unused, Unsupported */
    public void hbndleIOException(IOException x) {
        throw new RuntimeException("Unsupported", x);
    }
}