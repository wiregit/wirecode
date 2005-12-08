pbckage com.limegroup.gnutella.connection;

import jbva.io.IOException;
import jbva.nio.ByteBuffer;
import jbva.nio.ByteOrder;
import jbva.nio.channels.ReadableByteChannel;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.io.ChannelReadObserver;

/**
 * Rebds messages from a channel.  This class is notified when more of a message
 * cbn potentially be read by its handleRead() method being called.  To change
 * the chbnnel this reads from, use setReaderChannel(ReadableByteChannel).
 *
 * It is possible to construct this clbss without an initial source channel.
 * However, before hbndleRead is called, the channel must be set.
 *
 * The first time the chbnnel returns -1 this will throw an IOException, as it
 * never expects the chbnnel to run out of data.  Upon each read notification,
 * bs much data as possible will be read from the source channel.
 */
public clbss MessageReader implements ChannelReadObserver {
    
    /** the mbximum size of a message payload that we'll accept */
    privbte static final long MAX_MESSAGE_SIZE = 64 * 1024;
    /** the size of the hebder */
    privbte static final int HEADER_SIZE = 23;
    /** where in the hebder the payload is */
    privbte static final int PAYLOAD_LENGTH_OFFSET = 19;
    
    /** the constbnt buffer to use for emtpy payloads. */
    privbte static final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.allocate(0);
    
    /** the sole buffer for pbrsing msg headers */
    privbte final ByteBuffer header;
    /** the buffer used for pbrsing the payload -- recreated for each message */
    privbte ByteBuffer payload;
    
    /** the sole receiver of messbges */
    privbte final MessageReceiver receiver;
    /** the source chbnnel */
    privbte ReadableByteChannel channel;
    
    /** whether or not this rebder has been shut down yet. */
    privbte boolean shutdown = false;
    
    /**
     * Constructs b new MessageReader without an underlying source.
     * Prior to hbndleRead() being called, setReadChannel(ReadableByteChannel)
     * MUST be cblled.
     */    
    public MessbgeReader(MessageReceiver receiver) {
        this(null, receiver);
    }
    
    /**
     * Constructs b new MessageReader with the given source channel & receiver.
     */
    public MessbgeReader(ReadableByteChannel channel, MessageReceiver receiver) {
        if(receiver == null)
            throw new NullPointerException("null receiver");
            
        this.chbnnel = channel;
        this.receiver = receiver;
        this.hebder = ByteBuffer.allocate(HEADER_SIZE);
        hebder.order(ByteOrder.LITTLE_ENDIAN);
        this.pbyload = null;
    }
    
    /**
     * Sets the new chbnnel to be reading from.
     */
    public void setRebdChannel(ReadableByteChannel channel) {
        if(chbnnel == null)
            throw new NullPointerException("cbnnot set null channel!");
        
        this.chbnnel = channel;
    }
    
    /**
     * Gets the chbnnel that is used for reading.
     */
    public RebdableByteChannel getReadChannel() {
        return chbnnel;
    }
    
    /**
     * Notificbtion that a read can be performed from the given channel.
     * All messbges that can be read without blocking are read & dispatched.
     */
    public void hbndleRead() throws IOException {
        // Continue rebding until we can't fill up the header or payload.
        while(true) {
            int rebd = 0;
            
            // First try to fill up the hebder.
            while(hebder.hasRemaining() && (read = channel.read(header)) > 0);
            
            // If there hebder's not full, we can't bother reading the payload, so abort.
            if(hebder.hasRemaining()) {
                if(rebd == -1)
                    throw new IOException("EOF");
                brebk;
            }
                
            // if we hbven't set up a payload yet, set one up (if necessary).
            if(pbyload == null) {
                int pbyloadLength = header.getInt(PAYLOAD_LENGTH_OFFSET);
                
                if(pbyloadLength < 0 || payloadLength > MAX_MESSAGE_SIZE)
                    throw new IOException("should i implement skipping?");
                
                if(pbyloadLength == 0) {
                    pbyload = EMPTY_PAYLOAD;
                } else {
                    try {
                        pbyload = ByteBuffer.allocate(payloadLength);
                    } cbtch(OutOfMemoryError oome) {
                        throw new IOException("messbge too large.");
                    }
                }
            }
            
            // Okby, a payload is set up, let's read into it.
            while(pbyload.hasRemaining() && (read = channel.read(payload)) > 0);
            
            // If the pbyload's not full, we can't create a message, so abort.
            if(pbyload.hasRemaining()) {
                if(rebd == -1)
                    throw new IOException("eof");
                brebk;
            }
                
            // Yby, we've got a full message.
            try {
                Messbge m = Message.createMessage(header.array(), payload.array(), 
                                                  receiver.getSoftMbx(), receiver.getNetwork());
                receiver.processRebdMessage(m);
            } cbtch(BadPacketException ignored) {}
            
            if(rebd == -1)
                throw new IOException("eof");
            
            pbyload = null;
            hebder.clear();
        }
    }
    
    /** 
     * Informs the receiver thbt the message is shutdown.
     */
    public void shutdown() {
        synchronized(this) {
            if(shutdown)
                return;
                
            shutdown = true;
        }
        receiver.messbgingClosed();
    }
    
    /** Unused */
    public void hbndleIOException(IOException iox) {
        throw new RuntimeException("unsupported operbtion", iox);
    }
    
}
    
    