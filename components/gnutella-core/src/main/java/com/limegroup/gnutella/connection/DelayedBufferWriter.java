package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.InterestWriteChannel;
import com.limegroup.gnutella.io.Shutdownable;
import com.limegroup.gnutella.io.WriteObserver;

public class DelayedBufferWriter implements ChannelWriter, InterestWriteChannel {

    private final static int MAX_TIME = 200;
    
    private static final Log LOG = LogFactory.getLog(DelayedBufferWriter.class);
    
    /** The channel to write to & interest on. */    
    private volatile InterestWriteChannel sink;
    /** The next observer. */
    private volatile WriteObserver observer;
    
    /**
     * The buffer where we store delayed data.  Most of the time it will be
     * written to, so we keep it in compacted state by default.
     */
    private final ByteBuffer buf;
    
    private long lastFlushTime;
    
    public DelayedBufferWriter(int size) {
        buf = ByteBuffer.allocate(size);
    }

    public synchronized void interest(WriteObserver observer, boolean status) {
        this.observer = status ? observer : null;
        
        InterestWriteChannel source = sink;
        if(source != null)
            source.interest(this, true); 
    }

    public void close() throws IOException {
        Channel chan = sink;
        if(chan != null)
            chan.close();
    }

    public boolean isOpen() {
        Channel chan = sink;
        return chan != null ? chan.isOpen() : false;
    }

    public InterestWriteChannel getWriteChannel() {
        return sink;
    }

    public void setWriteChannel(InterestWriteChannel newChannel) {
        sink = newChannel;
        newChannel.interest(this,true);
    }

    public void handleIOException(IOException iox) {
        throw new RuntimeException("Unsupported", iox);
    }

    public void shutdown() {
        Shutdownable listener = observer;
        if(listener != null)
            listener.shutdown();
    }

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
        LOG.debug("buffered "+(buffer.position() - originalPos));
        return buffer.position() - originalPos;
    }

    public boolean handleWrite() throws IOException {
        LOG.debug("handling write for "+sink);
        
        WriteObserver upper = observer;
        if (upper != null) {
            LOG.debug("copying write event up");
            upper.handleWrite();
        }
        
        long now = System.currentTimeMillis();
        if (now - lastFlushTime > MAX_TIME) {
            LOG.debug("flush in time "+sink);
            flush(now);
        }
                 
        if (buf.position() == 0) {
            synchronized(this) {
                upper = observer;
                if (upper == null)
                    sink.interest(this,false);
            }
            
        }
        
        // not interested - no data left to write, and nobody above me
        LOG.debug("exiting write signal "+ buf.position());
        return true;
    }
    
    private void flush(long now) throws IOException {
        LOG.debug("trying to flush "+buf);
        buf.flip();
        InterestWriteChannel chan = sink;
        
        chan.write(buf);

        // if we wrote anything, consider this flushed
        if (buf.position() > 0) {
            LOG.debug("flushed");
            lastFlushTime = now;
            buf.compact();
        } else 
            buf.position(buf.limit()).limit(buf.capacity());
        
        LOG.debug(chan +" wrote buf: "+buf);
    }

}
