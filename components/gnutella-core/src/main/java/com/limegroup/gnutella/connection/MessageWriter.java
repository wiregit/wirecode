package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.io.*;
import com.limegroup.gnutella.util.BufferByteArrayOutputStream;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;

/** 
 * Writes messages using non-blocking I/O.
 *
 * Messages are queued via send(Message).  When a write can happen, this is notified
 * via handleWrite(), which will pull any non-expired messages from the queue, writing
 * them.  ConnectionStats are kept updated for all should-be-sent messages as well
 * as dropped messages (from expiry or buffer overflow), and the SentMessageHandler
 * is notified of all succesfully sent messages.
 */
public class MessageWriter implements ChannelWriter, OutputRunner {
    
    /**
     * The queue that holds the messages to write.  The queue internally can
     * expire messages which are old, or purge messages if many become buffered.
     */
    private final MessageQueue queue;
    
    /**
     * The OutputStream that messages are written to.  For efficieny, the stream
     * internally uses a ByteBuffer and we get the buffer directly to write to
     * our sink channel.  This prevents recreation of many byte[]s.
     */
    private final BufferByteArrayOutputStream out;
    
    /**
     * The statistics object that keeps track of how many messages were sent,
     * how many tried to be sent, how many dropped, etc...
     */
    private final ConnectionStats stats;
    
    /**
     * A callback for handlers who wish to process messages we succesfully sent.
     */
    private final SentMessageHandler sendHandler;
    
    /**
     * The sink channel we write to & interest ourselves on.
     */
    private volatile InterestWriteChannel channel;
    
    /**
     * Whether or not we've flipped the data.  This is an optimization so
     * we don't have to compact (which does array copies) as much.
     */
    private boolean flipped = false;
    
    /**
     * Constructs a new MessageWriter with the given stats, queue & sendHandler.
     * You MUST call setWriteChannel prior to handleWrite.
     */
    public MessageWriter(ConnectionStats stats, MessageQueue queue, SentMessageHandler sendHandler) {
        this.stats = stats;
        this.queue = queue;
        this.sendHandler = sendHandler;
        out = new BufferByteArrayOutputStream();
    }
    
    /** The channel we're writing to. */
    public InterestWriteChannel getWriteChannel() {
        return channel;
    }
    
    /** The channel we're writing to. */
    public void setWriteChannel(InterestWriteChannel channel) {
        this.channel = channel;
        channel.interest(this, true);
    }
    
    /**
     * Adds a new message to the queue.
     *
     * Any messages that were dropped because this was added are calculated
     * into the ConnectionStats.  The sink channel is notified that we're
     * interested in writing.
     */
    public synchronized void send(Message m) {
        stats.addSent();
        queue.add(m);
        int dropped = queue.resetDropped();
        stats.addSentDropped(dropped);
            
        InterestWriteChannel source = channel;
        if(source != null)
            source.interest(this, true);
    }
        
    /**
     * Writes as many messages as possible to the sink.
     */
    public boolean handleWrite() throws IOException {
        InterestWriteChannel source = channel;
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
                m.writeQuickly(out);
                sendHandler.processSentMessage(m);
                if(writeRemaining(source))
                    break;
            }
        }
        
        boolean remaining = flipped || !queue.isEmpty();
        if(!remaining) {
            source.interest(this, false);
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Writes any data that was left in the buffer.  As an optimization,
     * we do not recompact the buffer if more data can be written.  Instead,
     * we just wait till we can completely write the buffer & then clear it
     * entirely.  This prevents the need to compact the buffer.
     */
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
            while(buffer.hasRemaining() && source.write(buffer) > 0);
            
            // if we couldn't write everything, exit.
            if(buffer.hasRemaining())
                return true; // still have data to write.
                
            flipped = false;
            buffer.clear();
        }
        return false; // wrote everything.
    }
    
    /**
     * Ignored -- we'll shut down from reading.
     *
     * THIS MUST NOT CLOSE THE CONNECTION.  (Connection.close calls this.)
     */
    public void shutdown() {}
    
    /** Unused, Unsupported */
    public void handleIOException(IOException x) {
        throw new RuntimeException("Unsupported", x);
    }
}