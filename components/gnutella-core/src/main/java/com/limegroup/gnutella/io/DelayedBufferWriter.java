package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.Periodic;
import com.limegroup.gnutella.util.SchedulingThreadPool;

/**
 * A Writer that stores data within a buffer and writes it out after some delay,
 * or if the buffer fills up.
 */
public class DelayedBufferWriter implements ChannelWriter, InterestWriteChannel {

    private static final Log LOG = LogFactory.getLog(DelayedBufferWriter.class);

    /** The default delay time to use before forcing a flush */
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
    
    /** The delay time to use before forcing a flush */
    private final long delay;
    
    private final Periodic interester;
    
    /** The last time we flushed, so we don't flush again too soon. */
    private long lastFlushTime;
    
    /** Constructs a new DelayedBufferWriter whose buffer is the given size. */
    public DelayedBufferWriter(int size) {
    	this(size, MAX_TIME);
    }
    
    /** Constructs a new DelayedBufferWriter whose buffer is the given size and delay. */
    public DelayedBufferWriter(int size, long delay) {
    	this(size, delay, NIODispatcher.instance().getSchedulingThreadPool());
    }
    
    DelayedBufferWriter(int size, long delay, SchedulingThreadPool scheduler) {
    	buf = ByteBuffer.allocate(size);
    	this.delay = delay;
    	this.interester = new Periodic(
    			new Interester(),
    			scheduler);
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
    	if (status) {
    		this.observer = observer;
    		interester.unschedule();
    		LOG.debug("cancelling scheduled flush");
    	}
    	else 
    		this.observer = null;
    	
        InterestWriteChannel source = sink;
        if(source != null)
            source.interest(this, true); 
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
     * ago, we force a flush to occur.  We also force a flush if the observer is no
     * longer interested to make sure its last data is flushed from the buffer.
     */
    public boolean handleWrite() throws IOException {
        WriteObserver upper = observer;
        if (upper != null)
            upper.handleWrite();
        
        long now = System.currentTimeMillis();
        if (lastFlushTime == 0)
            lastFlushTime = now;
        if (now - lastFlushTime > delay) 
            flush(now);
                 
        synchronized(this) {
        	// It is possible that between the above check for 
        	// interested.handleWrite & here, we got pre-empted 
        	// and another thread turned on interest.
        	upper = observer;
        	if (upper == null) {
        		sink.interest(this,false);
        		
        		// If still no data after that, we've written everything we want -- exit.
        		if (!hasBufferedData()) 
        			return false;
        		else {
        			// otherwise schedule a flushing event.
        			interester.rescheduleIfLater(lastFlushTime + delay - now);
        		}
        	}
        } 
        
        return true;
    }
    
    /**
     * Writes data to the underlying channel, remembering the time we did this
     * if anything was written.  THIS DOES NOT BLOCK, NOR DOES IT ENFORCE
     * THAT ALL DATA WILL BE WRITTEN, UNLIKE OutputStream.flush().
     * 
     * @return true if the buffer is now empty
     */
    public boolean flush() throws IOException {
    	flush(System.currentTimeMillis());
    	return !hasBufferedData();
    }
    
    private void flush(long now) throws IOException {
        buf.flip();
        InterestWriteChannel chan = sink;
        
        chan.write(buf);

        // if we wrote anything, consider this flushed
        if (hasBufferedData()) {
            lastFlushTime = now;
            if (buf.hasRemaining())
                buf.compact();
            else
                buf.clear();
        } else  {
            buf.position(buf.limit()).limit(buf.capacity());
        }
    }
    
    private boolean hasBufferedData() {
    	return buf.position() > 0;
    }
    
    private class Interester implements Runnable {
    	public void run() {
    		DelayedBufferWriter me = DelayedBufferWriter.this;
    		synchronized(me) {
    			InterestWriteChannel below = me.sink;
    			WriteObserver above = observer;
    			if (below != null && 
    					below.isOpen() && 
    					above == null && 
    					buf.position() > 0) {
    				LOG.debug("forcing a flush");
    				below.interest(me, true);
    			}
    		}
    	}
    }
}
