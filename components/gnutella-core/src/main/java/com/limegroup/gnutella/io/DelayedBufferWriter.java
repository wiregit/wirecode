
// Edited for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.io.WriteObserver;

/**
 * A Writer that stores data within a buffer and writes it out after some delay,
 * or if the buffer fills up.
 * 
 * 
 * Extends and Implements
 * ChannelWriter:        This object has a sink channel it writes to, setWriteChannel() and getWriteChannel().
 * InterestWriteChannel: The object that gives us data can tell us it has some, interest().
 */
public class DelayedBufferWriter implements ChannelWriter, InterestWriteChannel {

    private static final Log LOG = LogFactory.getLog(DelayedBufferWriter.class);

    /** The delay time to use before forcing a flush */
    private final static int MAX_TIME = 200;
   
    /** The channel to write to & interest on. */    
    private volatile InterestWriteChannel sink;
    /** The next observer. */
    private volatile WriteObserver observer;
    
    /**
     * The buffer where we store delayed data.  Most of the time it will be
     * written to, so we keep it in compacted state by default.
     */
    private final ByteBuffer buf;
    
    /** The last time we flushed, so we don't flush again too soon. */
    private long lastFlushTime;
    
    /** Constructs a new DelayedBufferWriter whose buffer is the given size. */
    public DelayedBufferWriter(int size) {
        buf = ByteBuffer.allocate(size);
    }

    /**
     * Used by an observer to interest themselves in when something can
     * write to this.
     *
     * We must synchronize interest setting so that in the writing loop
     * we can ensure that interest isn't turned on between the time we
     * get the interested party, check for null, and turn off interest
     * (if it was null).
     */
    public synchronized void interest(WriteObserver observer, boolean status) {
        this.observer = status ? observer : null;
        
        InterestWriteChannel s = sink;
        if(s != null)
            s.interest(this, true); 
    }

    /** Closes the underlying channel. */
    public void close() throws IOException {
        Channel chan = sink;
        if(chan != null)
            chan.close();
    }

    /** Determines if the underlying channel is open. */
    public boolean isOpen() {
        Channel chan = sink;
        return chan != null ? chan.isOpen() : false;
    }

    /** Retreives the sink. */
    public InterestWriteChannel getWriteChannel() {
        return sink;
    }

    /** Sets the sink. */
    public void setWriteChannel(InterestWriteChannel newChannel) {
        sink = newChannel;
        newChannel.interest(this,true);
    }

    /** Unused, Unsupported */
    public void handleIOException(IOException iox) {
        throw new RuntimeException("Unsupported", iox);
    }

    /** Shuts down the last observer. */
    public void shutdown() {
        Shutdownable listener = observer;
        if(listener != null)
            listener.shutdown();
    }

    /**
     * Writes data into the internal buffer.
     *
     * If the internal buffer gets filled, it tries flushing some data out
     * to the sink.  If some data can be flushed, this continues filling the
     * internal buffer.  This continues forever until either the incoming
     * buffer is emptied or no data can be written to the sink.
     */
    public int write(ByteBuffer buffer) throws IOException {
        int originalPos = buffer.position();
        while(buffer.hasRemaining()) {
            if(buf.hasRemaining()) {
                int remaining = buf.remaining();
                int adding = buffer.remaining();
                if(remaining >= adding) {
                    buf.put(buffer);
                } else {
                    int oldLimit = buffer.limit();
                    int position = buffer.position();
                    buffer.limit(position + remaining);
                    buf.put(buffer);
                    buffer.limit(oldLimit);
                }
            } else {
                flush(System.currentTimeMillis());
                if (!buf.hasRemaining()) 
                    break;
            }
        }
        return buffer.position() - originalPos;
    }

    /**
     * Notification that a write can happen.  The observer is informed of the event
     * in order to try filling our internal buffer.  If our last flush was too long
     * ago, we force a flush to occur.
     */
    public boolean handleWrite() throws IOException {
        WriteObserver upper = observer;
        if (upper != null)
            upper.handleWrite();
        
        long now = System.currentTimeMillis();
        if (lastFlushTime == 0)
            lastFlushTime = now;
        if (now - lastFlushTime > MAX_TIME)
            flush(now);
                 
        // If still no data after that, we've written everything we want -- exit.
        if (buf.position() == 0) {
            // We have nothing left to write, however, it is possible
            // that between the above check for interested.handleWrite & here,
            // we got pre-empted and another thread turned on interest.
            synchronized(this) {
                upper = observer;
                if (upper == null)
                    sink.interest(this,false);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Writes data to the underlying channel, remembering the time we did this
     * if anything was written.  THIS DOES NOT BLOCK, NOR DOES IT ENFORCE
     * THAT ALL DATA WILL BE WRITTEN, UNLIKE OutputStream.flush().
     */
    private void flush(long now) throws IOException {
        buf.flip();
        InterestWriteChannel chan = sink;
        
        chan.write(buf);

        // if we wrote anything, consider this flushed
        if (buf.position() > 0) {
            lastFlushTime = now;
            if (buf.hasRemaining())
                buf.compact();
            else
                buf.clear();
        } else  {
            buf.position(buf.limit()).limit(buf.capacity());
        }
    }

}
