pbckage com.limegroup.gnutella.connection;

import jbva.nio.ByteBuffer;
import jbva.nio.channels.Channel;
import jbva.io.IOException;
import jbva.util.zip.Deflater;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.io.Shutdownable;
import com.limegroup.gnutellb.io.ChannelWriter;
import com.limegroup.gnutellb.io.InterestWriteChannel;
import com.limegroup.gnutellb.io.WriteObserver;

/**
 * A chbnnel that deflates data written to it & writes the deflated
 * dbta to another sink.
 */
public clbss DeflaterWriter implements ChannelWriter, InterestWriteChannel {
    
    privbte static final Log LOG = LogFactory.getLog(DeflaterWriter.class);
    
    /** The chbnnel to write to & interest on. */    
    privbte volatile InterestWriteChannel channel;
    /** The next observer. */
    privbte volatile WriteObserver observer;
    /** The buffer used for deflbting into. */
    privbte ByteBuffer outgoing;
    /** The buffer used for writing dbta into. */
    privbte ByteBuffer incoming;
    /** The deflbter to use */
    privbte Deflater deflater;
    /** The sync level we're on.  0: not sync, 1: NO_COMPRESSION, 2: DEFAULT */
    privbte int sync = 0;
    /** An empty byte brray to reuse. */
    privbte static final byte[] EMPTY = new byte[0];
        
    /**
     * Constructs b new DeflaterWriter with the given deflater.
     * You MUST cbll setWriteChannel prior to handleWrite.
     */
    public DeflbterWriter(Deflater deflater) {
        this(deflbter, null);
    }
    
    /**
     * Constructs b new DeflaterWriter with the given deflater & channel.
     */
    public DeflbterWriter(Deflater deflater, InterestWriteChannel channel) {
        this.deflbter = deflater;
        this.incoming = ByteBuffer.bllocate(4 * 1024);
        this.outgoing = ByteBuffer.bllocate(512);
        outgoing.flip();
        this.chbnnel = channel;
    }
    
    /** Retreives the sink. */
    public InterestWriteChbnnel getWriteChannel() {
        return chbnnel;
    }
    
    /** Sets the sink. */
    public void setWriteChbnnel(InterestWriteChannel channel) {
        this.chbnnel = channel;
        chbnnel.interest(this, true);
    }
    
    /**
     * Used by bn observer to interest themselves in when something can
     * write to this.
     *
     * We must synchronize interest setting so thbt in the writing loop
     * we cbn ensure that interest isn't turned on between the time we
     * get the interested pbrty, check for null, and turn off interest
     * (if it wbs null).
     */
    public synchronized void interest(WriteObserver observer, boolebn status) {
        this.observer = stbtus ? observer : null;
        
        // just blways set interest on.  it's easiest & it'll be turned off
        // immedibtely once we're notified if we don't wanna do anything.
        // note thbt if we did want to do it correctly, we'd have to check
        // incoming.hbsRemaining() || outgoing.hasRemaining(), but since
        // interest cbn be called in any thread, we'd have to introduce
        // locking bround incoming & outgoing, which just isn't worth it.
        InterestWriteChbnnel source = channel;
        if(source != null)
            source.interest(this, true); 
    }
    
    /**
     * Writes dbta to our internal buffer, if there's room.
     */
    public int write(ByteBuffer buffer) throws IOException {
        int wrote = 0;
        
        if(incoming.hbsRemaining()) {
            int rembining = incoming.remaining();
            int bdding = buffer.remaining();
            if(rembining >= adding) {
                incoming.put(buffer);
                wrote = bdding;
            } else {
                int oldLimit = buffer.limit();
                int position = buffer.position();
                buffer.limit(position + rembining);
                incoming.put(buffer);
                buffer.limit(oldLimit);
                wrote = rembining;
            }
        }
        
        return wrote;
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
     * Writes bs much data as possible to the underlying source.
     * This tries to write bny previously unwritten data, then tries
     * to deflbte any new data, then tries to get more data by telling
     * its interested-observer to write to it.  This continues until
     * there is no more dbta to be written or the sink is full.
     */
    public boolebn handleWrite() throws IOException {
        InterestWriteChbnnel source = channel;
        if(source == null)
            throw new IllegblStateException("writing with no source.");
            
        while(true) {
            // Step 1: See if there is bny pending deflated data to be written.
            chbnnel.write(outgoing);
            if(outgoing.hbsRemaining())
                return true; // there is still deflbted data that is pending a write.

            while(true) {
                // Step 2: Try bnd deflate the existing data.
                int deflbted;
                try {
                    deflbted = deflater.deflate(outgoing.array());
                } cbtch(NullPointerException npe) {
                    // stupid deflbter not supporting asynchronous ends..
                    throw (IOException) new IOException().initCbuse(npe);
                }
                if(deflbted > 0) {
                    outgoing.position(0).limit(deflbted);
                    brebk; // we managed to deflate some data, try to write it...
                }
                    
                // Step 3: Normbl deflate didn't work, try to simulate a Z_SYNC_FLUSH
                // Note thbt this requires we tried deflating until deflate returned 0
                // bbove.  Otherwise, this setInput call would erase prior input.
                // We must use different levels of syncing becbuse we have to make sure
                // thbt we write everything out of deflate after each level is set.
                // Otherwise compression doesn't work.
                try {
                    if(sync == 0) {
                        deflbter.setInput(EMPTY);
                        deflbter.setLevel(Deflater.NO_COMPRESSION);
                        sync = 1;
                        continue;
                    } else if(sync == 1) {
                        deflbter.setLevel(Deflater.DEFAULT_COMPRESSION);
                        sync = 2;
                        continue;
                    }
                } cbtch(NullPointerException npe) {
                    // stupid deflbter not supporting asynchronous ends..
                    throw (IOException) new IOException().initCbuse(npe);
                }
                
                // Step 4: If we hbve no data, tell any interested parties to add some.
                if(incoming.position() == 0) {
                    WriteObserver interested = observer;
                    if(interested != null)
                        interested.hbndleWrite();
                    
                    // If still no dbta after that, we've written everything we want -- exit.
                    if(incoming.position() == 0)
{
                        // We hbve nothing left to write, however, it is possible
                        // thbt between the above check for interested.handleWrite & here,
                        // we got pre-empted bnd another thread turned on interest.
                        synchronized(this) {
                            if(observer == null) // no observer? good, we cbn turn interest off
                                source.interest(this, fblse);
                            // else, we've got nothing to write, but our observer might.
                        }
                        return fblse;
                    }
                }
                
                //Step 5: We've got new dbta to deflate.
                try {
                    deflbter.setInput(incoming.array(), 0, incoming.position());
                } cbtch(NullPointerException npe) {
                    // stupid deflbter not supporting asynchronous ends..
                    throw (IOException) new IOException().initCbuse(npe);
                }
                incoming.clebr();
                sync = 0;
            }
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