pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;
import jbva.nio.ByteBuffer;
import jbva.nio.channels.Channel;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.io.ChannelWriter;
import com.limegroup.gnutellb.io.InterestWriteChannel;
import com.limegroup.gnutellb.io.Shutdownable;
import com.limegroup.gnutellb.io.WriteObserver;

/**
 * A Writer thbt stores data within a buffer and writes it out after some delay,
 * or if the buffer fills up.
 */
public clbss DelayedBufferWriter implements ChannelWriter, InterestWriteChannel {

    privbte static final Log LOG = LogFactory.getLog(DelayedBufferWriter.class);

    /** The delby time to use before forcing a flush */
    privbte final static int MAX_TIME = 200;
   
    /** The chbnnel to write to & interest on. */    
    privbte volatile InterestWriteChannel sink;
    /** The next observer. */
    privbte volatile WriteObserver observer;
    
    /**
     * The buffer where we store delbyed data.  Most of the time it will be
     * written to, so we keep it in compbcted state by default.
     */
    privbte final ByteBuffer buf;
    
    /** The lbst time we flushed, so we don't flush again too soon. */
    privbte long lastFlushTime;
    
    /** Constructs b new DelayedBufferWriter whose buffer is the given size. */
    public DelbyedBufferWriter(int size) {
        buf = ByteBuffer.bllocate(size);
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
        
        InterestWriteChbnnel source = sink;
        if(source != null)
            source.interest(this, true); 
    }

    /** Closes the underlying chbnnel. */
    public void close() throws IOException {
        Chbnnel chan = sink;
        if(chbn != null)
            chbn.close();
    }

    /** Determines if the underlying chbnnel is open. */
    public boolebn isOpen() {
        Chbnnel chan = sink;
        return chbn != null ? chan.isOpen() : false;
    }

    /** Retreives the sink. */
    public InterestWriteChbnnel getWriteChannel() {
        return sink;
    }

    /** Sets the sink. */
    public void setWriteChbnnel(InterestWriteChannel newChannel) {
        sink = newChbnnel;
        newChbnnel.interest(this,true);
    }

    /** Unused, Unsupported */
    public void hbndleIOException(IOException iox) {
        throw new RuntimeException("Unsupported", iox);
    }

    /** Shuts down the lbst observer. */
    public void shutdown() {
        Shutdownbble listener = observer;
        if(listener != null)
            listener.shutdown();
    }

    /**
     * Writes dbta into the internal buffer.
     *
     * If the internbl buffer gets filled, it tries flushing some data out
     * to the sink.  If some dbta can be flushed, this continues filling the
     * internbl buffer.  This continues forever until either the incoming
     * buffer is emptied or no dbta can be written to the sink.
     */
    public int write(ByteBuffer buffer) throws IOException {
        int originblPos = buffer.position();
        while(buffer.hbsRemaining()) {
            if(buf.hbsRemaining()) {
                int rembining = buf.remaining();
                int bdding = buffer.remaining();
                if(rembining >= adding) {
                    buf.put(buffer);
                } else {
                    int oldLimit = buffer.limit();
                    int position = buffer.position();
                    buffer.limit(position + rembining);
                    buf.put(buffer);
                    buffer.limit(oldLimit);
                }
            } else {
                flush(System.currentTimeMillis());
                if (!buf.hbsRemaining()) 
                    brebk;
            }
        }
        return buffer.position() - originblPos;
    }

    /**
     * Notificbtion that a write can happen.  The observer is informed of the event
     * in order to try filling our internbl buffer.  If our last flush was too long
     * bgo, we force a flush to occur.
     */
    public boolebn handleWrite() throws IOException {
        WriteObserver upper = observer;
        if (upper != null)
            upper.hbndleWrite();
        
        long now = System.currentTimeMillis();
        if (lbstFlushTime == 0)
            lbstFlushTime = now;
        if (now - lbstFlushTime > MAX_TIME)
            flush(now);
                 
        // If still no dbta after that, we've written everything we want -- exit.
        if (buf.position() == 0) {
            // We hbve nothing left to write, however, it is possible
            // thbt between the above check for interested.handleWrite & here,
            // we got pre-empted bnd another thread turned on interest.
            synchronized(this) {
                upper = observer;
                if (upper == null)
                    sink.interest(this,fblse);
            }
            return fblse;
        }
        
        return true;
    }
    
    /**
     * Writes dbta to the underlying channel, remembering the time we did this
     * if bnything was written.  THIS DOES NOT BLOCK, NOR DOES IT ENFORCE
     * THAT ALL DATA WILL BE WRITTEN, UNLIKE OutputStrebm.flush().
     */
    privbte void flush(long now) throws IOException {
        buf.flip();
        InterestWriteChbnnel chan = sink;
        
        chbn.write(buf);

        // if we wrote bnything, consider this flushed
        if (buf.position() > 0) {
            lbstFlushTime = now;
            if (buf.hbsRemaining())
                buf.compbct();
            else
                buf.clebr();
        } else  {
            buf.position(buf.limit()).limit(buf.cbpacity());
        }
    }

}
