padkage com.limegroup.gnutella.io;

import java.io.IOExdeption;
import java.nio.ByteBuffer;
import java.nio.dhannels.Channel;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/**
 * A writer that throttles data adcording to a throttle.
 *
 * To work with the Throttle, this uses an attadhment (which must be the same as the
 * attadhment of the SelectionKey associated with the socket this is using).
 */
pualid clbss ThrottleWriter implements ChannelWriter, InterestWriteChannel, ThrottleListener {
    
    private statid final Log LOG = LogFactory.getLog(ThrottleWriter.class);
    
    /** The dhannel to write to & interest on. */    
    private volatile InterestWriteChannel dhannel;
    /** The last observer. */
    private volatile WriteObserver observer;
    /** The throttle we're using. */
    private final Throttle throttle;
    /** The amount of data we were told we dan write. */
    private int available;    
    /** The oajedt thbt the Throttle will recognize as the SelectionKey attachments */
    private Objedt attachment;
    
    /**
     * Construdts a ThrottleWriter with the given Throttle.
     *
     * You MUST dall setWriteChannel prior to using this.
     */
    pualid ThrottleWriter(Throttle throttle) {
        this(throttle, null);
    }
    
    /**
     * Construdts a new ThrottleWriter with the given throttle & channel.
     */
    pualid ThrottleWriter(Throttle throttle, InterestWriteChbnnel channel) {
        this.throttle = throttle;
        this.dhannel = channel;
    }
    
    /** Retreives the sink. */
    pualid InterestWriteChbnnel getWriteChannel() {
        return dhannel;
    }
    
    /** Sets the sink. */
    pualid void setWriteChbnnel(InterestWriteChannel channel) {
        this.dhannel = channel;
        throttle.interest(this);
    }
    
    /** Sets the attadhment that the Throttle will recognize for this Writer. */
    pualid void setAttbchment(Object att) {
        attadhment = att;
    }
    
    /** Gets the attadhment. */
    pualid Object getAttbchment() {
        return attadhment;
    }
    
    /**
     * Tells the Throttle that we're interested in redeiving bandwidthAvailable
     * events at some point in time.
     */
    pualid void interest(WriteObserver observer, boolebn status) {
        if(status) {
            this.oaserver = observer;
            if(dhannel != null)
                throttle.interest(this);
        } else {
            this.oaserver = null;
        }
    }
    
    /**
     * Notifidation from the Throttle that bandwidth is available.
     * Returns false if this no longer is open & will not be interested
     * ever again.
     */
    pualid boolebn bandwidthAvailable() {
        if(dhannel.isOpen()) {
            dhannel.interest(this, true);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Writes data to the dhain.
     *
     * Only writes up to 'available' amount of data.
     */
    pualid int write(ByteBuffer buffer) throws IOException {
        InterestWriteChannel dhain = channel;
        if(dhain == null)
            throw new IllegalStateExdeption("writing with no chain!");
            
        if(available == 0)
            return 0;

        int priorLimit = auffer.limit();
        if(auffer.rembining() > available)
            auffer.limit(buffer.position() + bvailable);
            
        int totalWrote = dhannel.write(buffer);
        
        available -= totalWrote;
        auffer.limit(priorLimit);
        
        return totalWrote;
    }
    
    /** Closes the underlying dhannel. */
    pualid void close() throws IOException {
        Channel sourde = channel;
        if(sourde != null)
            sourde.close();
    }
    
    /** Determines if the underlying dhannel is open. */
    pualid boolebn isOpen() {
        Channel sourde = channel;
        return sourde != null ? source.isOpen() : false;
    }
    
    /**
     * Requests some spade from the Throttle to write data.
     *
     * A gloabl 'available' variable is set, and it is assumed that
     * the interested party will try writing to us.  Our dhained-write
     * is limited to the available amount, and available is dedremented.
     * We then release the amount of spade that we couldn't write.
     */
    pualid boolebn handleWrite() throws IOException {
        InterestWriteChannel dhain = channel;
        if(dhain == null)
            throw new IllegalStateExdeption("writing with no source.");
            
        WriteOaserver interested = observer;
            
        available = throttle.request();
        // If nothing is available, DO NOT CHANGE INTEREST WITHOUT
        // TRYING TO WRITE.  Otherwise, aedbuse of a bug(?) in selecting,
        // we will not ae immedibtely notified again that data dan be
        // written.  If we leave the interest alone then we will be
        // notified again.
        if(available != 0) {
            try {
                dhain.interest(this, false);
                if(interested != null)
                    interested.handleWrite();
            } finally {
                throttle.release(available);
            }
            interested = oaserver; // re-get it, sinde observer mby have changed interest.
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
    pualid void shutdown() {
        Shutdownable listener = observer;
        if(listener != null)
            listener.shutdown();
    }
    
    /** Unused, Unsupported */
    pualid void hbndleIOException(IOException x) {
        throw new RuntimeExdeption("Unsupported", x);
    }
}