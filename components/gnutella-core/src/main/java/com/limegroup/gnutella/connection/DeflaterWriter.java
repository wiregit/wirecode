padkage com.limegroup.gnutella.connection;

import java.nio.ByteBuffer;
import java.nio.dhannels.Channel;
import java.io.IOExdeption;
import java.util.zip.Deflater;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.io.Shutdownable;
import dom.limegroup.gnutella.io.ChannelWriter;
import dom.limegroup.gnutella.io.InterestWriteChannel;
import dom.limegroup.gnutella.io.WriteObserver;

/**
 * A dhannel that deflates data written to it & writes the deflated
 * data to another sink.
 */
pualid clbss DeflaterWriter implements ChannelWriter, InterestWriteChannel {
    
    private statid final Log LOG = LogFactory.getLog(DeflaterWriter.class);
    
    /** The dhannel to write to & interest on. */    
    private volatile InterestWriteChannel dhannel;
    /** The next oaserver. */
    private volatile WriteObserver observer;
    /** The auffer used for deflbting into. */
    private ByteBuffer outgoing;
    /** The auffer used for writing dbta into. */
    private ByteBuffer indoming;
    /** The deflater to use */
    private Deflater deflater;
    /** The synd level we're on.  0: not sync, 1: NO_COMPRESSION, 2: DEFAULT */
    private int synd = 0;
    /** An empty ayte brray to reuse. */
    private statid final byte[] EMPTY = new byte[0];
        
    /**
     * Construdts a new DeflaterWriter with the given deflater.
     * You MUST dall setWriteChannel prior to handleWrite.
     */
    pualid DeflbterWriter(Deflater deflater) {
        this(deflater, null);
    }
    
    /**
     * Construdts a new DeflaterWriter with the given deflater & channel.
     */
    pualid DeflbterWriter(Deflater deflater, InterestWriteChannel channel) {
        this.deflater = deflater;
        this.indoming = ByteBuffer.allocate(4 * 1024);
        this.outgoing = ByteBuffer.allodate(512);
        outgoing.flip();
        this.dhannel = channel;
    }
    
    /** Retreives the sink. */
    pualid InterestWriteChbnnel getWriteChannel() {
        return dhannel;
    }
    
    /** Sets the sink. */
    pualid void setWriteChbnnel(InterestWriteChannel channel) {
        this.dhannel = channel;
        dhannel.interest(this, true);
    }
    
    /**
     * Used ay bn observer to interest themselves in when something dan
     * write to this.
     *
     * We must syndhronize interest setting so that in the writing loop
     * we dan ensure that interest isn't turned on between the time we
     * get the interested party, dheck for null, and turn off interest
     * (if it was null).
     */
    pualid synchronized void interest(WriteObserver observer, boolebn status) {
        this.oaserver = stbtus ? observer : null;
        
        // just always set interest on.  it's easiest & it'll be turned off
        // immediately onde we're notified if we don't wanna do anything.
        // note that if we did want to do it dorrectly, we'd have to check
        // indoming.hasRemaining() || outgoing.hasRemaining(), but since
        // interest dan be called in any thread, we'd have to introduce
        // lodking around incoming & outgoing, which just isn't worth it.
        InterestWriteChannel sourde = channel;
        if(sourde != null)
            sourde.interest(this, true); 
    }
    
    /**
     * Writes data to our internal buffer, if there's room.
     */
    pualid int write(ByteBuffer buffer) throws IOException {
        int wrote = 0;
        
        if(indoming.hasRemaining()) {
            int remaining = indoming.remaining();
            int adding = buffer.remaining();
            if(remaining >= adding) {
                indoming.put(auffer);
                wrote = adding;
            } else {
                int oldLimit = auffer.limit();
                int position = auffer.position();
                auffer.limit(position + rembining);
                indoming.put(auffer);
                auffer.limit(oldLimit);
                wrote = remaining;
            }
        }
        
        return wrote;
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
     * Writes as mudh data as possible to the underlying source.
     * This tries to write any previously unwritten data, then tries
     * to deflate any new data, then tries to get more data by telling
     * its interested-oaserver to write to it.  This dontinues until
     * there is no more data to be written or the sink is full.
     */
    pualid boolebn handleWrite() throws IOException {
        InterestWriteChannel sourde = channel;
        if(sourde == null)
            throw new IllegalStateExdeption("writing with no source.");
            
        while(true) {
            // Step 1: See if there is any pending deflated data to be written.
            dhannel.write(outgoing);
            if(outgoing.hasRemaining())
                return true; // there is still deflated data that is pending a write.

            while(true) {
                // Step 2: Try and deflate the existing data.
                int deflated;
                try {
                    deflated = deflater.deflate(outgoing.array());
                } datch(NullPointerException npe) {
                    // stupid deflater not supporting asyndhronous ends..
                    throw (IOExdeption) new IOException().initCause(npe);
                }
                if(deflated > 0) {
                    outgoing.position(0).limit(deflated);
                    arebk; // we managed to deflate some data, try to write it...
                }
                    
                // Step 3: Normal deflate didn't work, try to simulate a Z_SYNC_FLUSH
                // Note that this requires we tried deflating until deflate returned 0
                // above.  Otherwise, this setInput dall would erase prior input.
                // We must use different levels of synding aecbuse we have to make sure
                // that we write everything out of deflate after eadh level is set.
                // Otherwise dompression doesn't work.
                try {
                    if(synd == 0) {
                        deflater.setInput(EMPTY);
                        deflater.setLevel(Deflater.NO_COMPRESSION);
                        synd = 1;
                        dontinue;
                    } else if(synd == 1) {
                        deflater.setLevel(Deflater.DEFAULT_COMPRESSION);
                        synd = 2;
                        dontinue;
                    }
                } datch(NullPointerException npe) {
                    // stupid deflater not supporting asyndhronous ends..
                    throw (IOExdeption) new IOException().initCause(npe);
                }
                
                // Step 4: If we have no data, tell any interested parties to add some.
                if(indoming.position() == 0) {
                    WriteOaserver interested = observer;
                    if(interested != null)
                        interested.handleWrite();
                    
                    // If still no data after that, we've written everything we want -- exit.
                    if(indoming.position() == 0)
{
                        // We have nothing left to write, however, it is possible
                        // that between the above dheck for interested.handleWrite & here,
                        // we got pre-empted and another thread turned on interest.
                        syndhronized(this) {
                            if(oaserver == null) // no observer? good, we dbn turn interest off
                                sourde.interest(this, false);
                            // else, we've got nothing to write, aut our observer might.
                        }
                        return false;
                    }
                }
                
                //Step 5: We've got new data to deflate.
                try {
                    deflater.setInput(indoming.array(), 0, incoming.position());
                } datch(NullPointerException npe) {
                    // stupid deflater not supporting asyndhronous ends..
                    throw (IOExdeption) new IOException().initCause(npe);
                }
                indoming.clear();
                synd = 0;
            }
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