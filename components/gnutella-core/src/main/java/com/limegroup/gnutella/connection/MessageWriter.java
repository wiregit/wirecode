package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.io.*;
import com.limegroup.gnutella.util.BufferByteArrayOutputStream;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;

/** 
 * Writes messages.
 */
public class MessageWriter implements ChannelWriter, OutputRunner {
    
    private final MessageQueue queue;
    private final BufferByteArrayOutputStream out;
    private final ConnectionStats stats;
    private final SentMessageHandler sendHandler;
    private volatile InterestWriteChannel channel;
    
    /**
     * whether or not we've flipped the data.  this is an optimization so
     * we don't have to compact (which does array copies) as much.
     */
    private boolean flipped = false;
    
    public MessageWriter(ConnectionStats stats, MessageQueue queue, SentMessageHandler sendHandler) {
        this.stats = stats;
        this.queue = queue;
        this.sendHandler = sendHandler;
        out = new BufferByteArrayOutputStream();
    }
    
    public InterestWriteChannel getWriteChannel() {
        return channel;
    }
    
    public void setWriteChannel(InterestWriteChannel channel) {
        this.channel = channel;
        channel.interest(this, true);
    }
    
    /** Adds a new message to the queue */
    public synchronized void send(Message m) {
        stats.addSent();
        queue.add(m);
        int dropped = queue.resetDropped();
        stats.addSentDropped(dropped);
        
        InterestWriteChannel source = channel;
        if(source != null)
            source.interest(this, true);
    }
        
    /** Adds as many queued messages as possible to the underlying source. */
    public boolean handleWrite() throws IOException {
        WritableByteChannel source = channel;
        if(source == null)
            throw new IllegalStateException("writing with no source.");
            
        // first try to write any leftover data without grabbing a lock.
        if(writeRemaining(source))
            return true;
            
        // then loop through and write to the channel till we can't anymore.
        synchronized(this) {
            while(true) {
                Message m = queue.removeNext();
                int dropped = queue.resetDropped();
                stats.addSentDropped(dropped);
                if(m == null)
                    break;
                m.write(out);
                sendHandler.processSentMessage(m);
                if(writeRemaining(source))
                    break;
            }
        }
        
        return flipped || !queue.isEmpty() || out.size() > 0;
    }
    
    private boolean writeRemaining(WritableByteChannel source) throws IOException {
        // if there was data left in the stream, try writing it.
        ByteBuffer buffer = out.buffer();
        
        // write any data that was leftover in the buffer.
        if(flipped || buffer.position() > 0) {
            // prepare for writing...
            if(!flipped) {
                buffer.flip();
                flipped = true;
            }

            // write.
            int wrote = 0;                
            while(buffer.hasRemaining() && (wrote = source.write(buffer)) > 0);
            
            // if we couldn't write everything, exit.
            if(wrote == 0 && buffer.hasRemaining())
                return true; // still have data to write.
                
            flipped = false;
            buffer.clear();
        }
        return false; // wrote everything.
    }
    
    /** Ignored -- we'll shut down from reading. */
    public void shutdown() { }
    
    /** Unused, Unsupported */
    public void handleIOException(IOException x) {
        throw new RuntimeException("Unsupported", x);
    }
}