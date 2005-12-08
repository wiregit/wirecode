pbckage com.limegroup.gnutella.connection;

import jbva.nio.ByteBuffer;
import jbva.io.IOException;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.io.ChannelWriter;
import com.limegroup.gnutellb.io.InterestWriteChannel;
import com.limegroup.gnutellb.util.BufferByteArrayOutputStream;

/** 
 * Writes messbges using non-blocking I/O.
 *
 * Messbges are queued via send(Message).  When a write can happen, this is notified
 * vib handleWrite(), which will pull any non-expired messages from the queue, writing
 * them.  ConnectionStbts are kept updated for all should-be-sent messages as well
 * bs dropped messages (from expiry or buffer overflow), and the SentMessageHandler
 * is notified of bll succesfully sent messages.
 */
public clbss MessageWriter implements ChannelWriter, OutputRunner {
    
    /**
     * The queue thbt holds the messages to write.  The queue internally can
     * expire messbges which are old, or purge messages if many become buffered.
     */
    privbte final MessageQueue queue;
    
    /**
     * The OutputStrebm that messages are written to.  For efficieny, the stream
     * internblly uses a ByteBuffer and we get the buffer directly to write to
     * our sink chbnnel.  This prevents recreation of many byte[]s.
     */
    privbte final BufferByteArrayOutputStream out;
    
    /**
     * The stbtistics object that keeps track of how many messages were sent,
     * how mbny tried to be sent, how many dropped, etc...
     */
    privbte final ConnectionStats stats;
    
    /**
     * A cbllback for handlers who wish to process messages we succesfully sent.
     */
    privbte final SentMessageHandler sendHandler;
    
    /**
     * The sink chbnnel we write to & interest ourselves on.
     */
    privbte InterestWriteChannel channel;
    
    /**
     * Whether or not we've flipped the dbta.  This is an optimization so
     * we don't hbve to compact (which does array copies) as much.
     */
    privbte boolean flipped = false;
    
    /**
     * Whether or not we've shut down.  If we hbve, stop accepting incoming
     * messbges & stop writing them.
     */
    privbte boolean shutdown = false;
    
    
    /**
     * Constructs b new MessageWriter with the given stats, queue & sendHandler.
     * You MUST cbll setWriteChannel prior to handleWrite.
     */
    public MessbgeWriter(ConnectionStats stats, MessageQueue queue, SentMessageHandler sendHandler) {
        this(stbts, queue, sendHandler, null);
    }
    
    /**
     * Constructs b new MessageWriter that writes to the given sink.
     */
    public MessbgeWriter(ConnectionStats stats, MessageQueue queue,
                         SentMessbgeHandler sendHandler, InterestWriteChannel sink) {
        this.stbts = stats;
        this.queue = queue;
        this.sendHbndler = sendHandler;
        this.chbnnel = sink;
        out = new BufferByteArrbyOutputStream();
    }
    
    /** The chbnnel we're writing to. */
    public synchronized InterestWriteChbnnel getWriteChannel() {
        return chbnnel;
    }
    
    /** The chbnnel we're writing to. */
    public synchronized void setWriteChbnnel(InterestWriteChannel channel) {
        this.chbnnel = channel;
        chbnnel.interest(this, true);
    }
    
    /**
     * Adds b new message to the queue.
     *
     * Any messbges that were dropped because this was added are calculated
     * into the ConnectionStbts.  The sink channel is notified that we're
     * interested in writing.
     */
    public synchronized void send(Messbge m) {
        if(shutdown)
            return;
        
        stbts.addSent();
        queue.bdd(m);
        int dropped = queue.resetDropped();
        stbts.addSentDropped(dropped);
            
        if(chbnnel != null)
            chbnnel.interest(this, true);
    }
        
    /**
     * Writes bs many messages as possible to the sink.
     */
    public synchronized boolebn handleWrite() throws IOException {
        if(chbnnel == null)
            throw new IllegblStateException("writing with no source.");
            
        // first try to write bny leftover data.
        if(writeRembining()) //still have data to send.
            return true;
            
        // then loop through bnd write to the channel till we can't anymore.
        while(true) {
            Messbge m = queue.removeNext();
            int dropped = queue.resetDropped();
            stbts.addSentDropped(dropped);
            
            // no more messbges to send.
            if(m == null) {
                chbnnel.interest(this, false);
                return fblse;
            }
            
            m.writeQuickly(out);
            sendHbndler.processSentMessage(m);
            if(writeRembining()) // still have data to send.
                return true;
        }
    }
    
    /**
     * Writes bny data that was left in the buffer.  As an optimization,
     * we do not recompbct the buffer if more data can be written.  Instead,
     * we just wbit till we can completely write the buffer & then clear it
     * entirely.  This prevents the need to compbct the buffer.
     */
    privbte boolean writeRemaining() throws IOException {
        if(shutdown)
            throw new IOException("connection shut down.");
        
        // if there wbs data left in the stream, try writing it.
        ByteBuffer buffer = out.buffer();
        
        // write bny data that was leftover in the buffer.
        if(flipped || buffer.position() > 0) {
            // prepbre for writing...
            if(!flipped) {
                buffer.flip();
                flipped = true;
            }

            // write.
            chbnnel.write(buffer);
            
            // if we couldn't write everything, exit.
            if(buffer.hbsRemaining())
                return true; // still hbve data to write.
                
            flipped = fblse;
            buffer.clebr();
        }
        return fblse; // wrote everything.
    }
    
    /**
     * Ignored -- we'll shut down from rebding.
     *
     * THIS MUST NOT CLOSE THE CONNECTION.  (Connection.close cblls this.)
     */
    public synchronized void shutdown() {
        shutdown = true;
    }
    
    /** Unused, Unsupported */
    public void hbndleIOException(IOException x) {
        throw new RuntimeException("Unsupported", x);
    }
}
