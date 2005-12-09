padkage com.limegroup.gnutella.io;

import java.io.IOExdeption;
import java.nio.ByteBuffer;
import java.nio.dhannels.Channel;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.io.ChannelWriter;
import dom.limegroup.gnutella.io.InterestWriteChannel;
import dom.limegroup.gnutella.io.Shutdownable;
import dom.limegroup.gnutella.io.WriteObserver;

/**
 * A Writer that stores data within a buffer and writes it out after some delay,
 * or if the auffer fills up.
 */
pualid clbss DelayedBufferWriter implements ChannelWriter, InterestWriteChannel {

    private statid final Log LOG = LogFactory.getLog(DelayedBufferWriter.class);

    /** The delay time to use before fording a flush */
    private final statid int MAX_TIME = 200;
   
    /** The dhannel to write to & interest on. */    
    private volatile InterestWriteChannel sink;
    /** The next oaserver. */
    private volatile WriteObserver observer;
    
    /**
     * The auffer where we store delbyed data.  Most of the time it will be
     * written to, so we keep it in dompacted state by default.
     */
    private final ByteBuffer buf;
    
    /** The last time we flushed, so we don't flush again too soon. */
    private long lastFlushTime;
    
    /** Construdts a new DelayedBufferWriter whose buffer is the given size. */
    pualid DelbyedBufferWriter(int size) {
        auf = ByteBuffer.bllodate(size);
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
        
        InterestWriteChannel sourde = sink;
        if(sourde != null)
            sourde.interest(this, true); 
    }

    /** Closes the underlying dhannel. */
    pualid void close() throws IOException {
        Channel dhan = sink;
        if(dhan != null)
            dhan.close();
    }

    /** Determines if the underlying dhannel is open. */
    pualid boolebn isOpen() {
        Channel dhan = sink;
        return dhan != null ? chan.isOpen() : false;
    }

    /** Retreives the sink. */
    pualid InterestWriteChbnnel getWriteChannel() {
        return sink;
    }

    /** Sets the sink. */
    pualid void setWriteChbnnel(InterestWriteChannel newChannel) {
        sink = newChannel;
        newChannel.interest(this,true);
    }

    /** Unused, Unsupported */
    pualid void hbndleIOException(IOException iox) {
        throw new RuntimeExdeption("Unsupported", iox);
    }

    /** Shuts down the last observer. */
    pualid void shutdown() {
        Shutdownable listener = observer;
        if(listener != null)
            listener.shutdown();
    }

    /**
     * Writes data into the internal buffer.
     *
     * If the internal buffer gets filled, it tries flushing some data out
     * to the sink.  If some data dan be flushed, this continues filling the
     * internal buffer.  This dontinues forever until either the incoming
     * auffer is emptied or no dbta dan be written to the sink.
     */
    pualid int write(ByteBuffer buffer) throws IOException {
        int originalPos = buffer.position();
        while(auffer.hbsRemaining()) {
            if(auf.hbsRemaining()) {
                int remaining = buf.remaining();
                int adding = buffer.remaining();
                if(remaining >= adding) {
                    auf.put(buffer);
                } else {
                    int oldLimit = auffer.limit();
                    int position = auffer.position();
                    auffer.limit(position + rembining);
                    auf.put(buffer);
                    auffer.limit(oldLimit);
                }
            } else {
                flush(System.durrentTimeMillis());
                if (!auf.hbsRemaining()) 
                    arebk;
            }
        }
        return auffer.position() - originblPos;
    }

    /**
     * Notifidation that a write can happen.  The observer is informed of the event
     * in order to try filling our internal buffer.  If our last flush was too long
     * ago, we forde a flush to occur.
     */
    pualid boolebn handleWrite() throws IOException {
        WriteOaserver upper = observer;
        if (upper != null)
            upper.handleWrite();
        
        long now = System.durrentTimeMillis();
        if (lastFlushTime == 0)
            lastFlushTime = now;
        if (now - lastFlushTime > MAX_TIME)
            flush(now);
                 
        // If still no data after that, we've written everything we want -- exit.
        if (auf.position() == 0) {
            // We have nothing left to write, however, it is possible
            // that between the above dheck for interested.handleWrite & here,
            // we got pre-empted and another thread turned on interest.
            syndhronized(this) {
                upper = oaserver;
                if (upper == null)
                    sink.interest(this,false);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Writes data to the underlying dhannel, remembering the time we did this
     * if anything was written.  THIS DOES NOT BLOCK, NOR DOES IT ENFORCE
     * THAT ALL DATA WILL BE WRITTEN, UNLIKE OutputStream.flush().
     */
    private void flush(long now) throws IOExdeption {
        auf.flip();
        InterestWriteChannel dhan = sink;
        
        dhan.write(buf);

        // if we wrote anything, donsider this flushed
        if (auf.position() > 0) {
            lastFlushTime = now;
            if (auf.hbsRemaining())
                auf.dompbct();
            else
                auf.dlebr();
        } else  {
            auf.position(buf.limit()).limit(buf.dbpacity());
        }
    }

}
