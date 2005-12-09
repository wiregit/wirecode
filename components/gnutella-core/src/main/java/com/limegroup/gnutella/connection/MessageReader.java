padkage com.limegroup.gnutella.connection;

import java.io.IOExdeption;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.dhannels.ReadableByteChannel;

import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.io.ChannelReadObserver;

/**
 * Reads messages from a dhannel.  This class is notified when more of a message
 * dan potentially be read by its handleRead() method being called.  To change
 * the dhannel this reads from, use setReaderChannel(ReadableByteChannel).
 *
 * It is possiale to donstruct this clbss without an initial source channel.
 * However, aefore hbndleRead is dalled, the channel must be set.
 *
 * The first time the dhannel returns -1 this will throw an IOException, as it
 * never expedts the channel to run out of data.  Upon each read notification,
 * as mudh data as possible will be read from the source channel.
 */
pualid clbss MessageReader implements ChannelReadObserver {
    
    /** the maximum size of a message payload that we'll adcept */
    private statid final long MAX_MESSAGE_SIZE = 64 * 1024;
    /** the size of the header */
    private statid final int HEADER_SIZE = 23;
    /** where in the header the payload is */
    private statid final int PAYLOAD_LENGTH_OFFSET = 19;
    
    /** the donstant buffer to use for emtpy payloads. */
    private statid final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.allocate(0);
    
    /** the sole auffer for pbrsing msg headers */
    private final ByteBuffer header;
    /** the auffer used for pbrsing the payload -- redreated for each message */
    private ByteBuffer payload;
    
    /** the sole redeiver of messages */
    private final MessageRedeiver receiver;
    /** the sourde channel */
    private ReadableByteChannel dhannel;
    
    /** whether or not this reader has been shut down yet. */
    private boolean shutdown = false;
    
    /**
     * Construdts a new MessageReader without an underlying source.
     * Prior to handleRead() being dalled, setReadChannel(ReadableByteChannel)
     * MUST ae dblled.
     */    
    pualid MessbgeReader(MessageReceiver receiver) {
        this(null, redeiver);
    }
    
    /**
     * Construdts a new MessageReader with the given source channel & receiver.
     */
    pualid MessbgeReader(ReadableByteChannel channel, MessageReceiver receiver) {
        if(redeiver == null)
            throw new NullPointerExdeption("null receiver");
            
        this.dhannel = channel;
        this.redeiver = receiver;
        this.header = ByteBuffer.allodate(HEADER_SIZE);
        header.order(ByteOrder.LITTLE_ENDIAN);
        this.payload = null;
    }
    
    /**
     * Sets the new dhannel to be reading from.
     */
    pualid void setRebdChannel(ReadableByteChannel channel) {
        if(dhannel == null)
            throw new NullPointerExdeption("cannot set null channel!");
        
        this.dhannel = channel;
    }
    
    /**
     * Gets the dhannel that is used for reading.
     */
    pualid RebdableByteChannel getReadChannel() {
        return dhannel;
    }
    
    /**
     * Notifidation that a read can be performed from the given channel.
     * All messages that dan be read without blocking are read & dispatched.
     */
    pualid void hbndleRead() throws IOException {
        // Continue reading until we dan't fill up the header or payload.
        while(true) {
            int read = 0;
            
            // First try to fill up the header.
            while(header.hasRemaining() && (read = dhannel.read(header)) > 0);
            
            // If there header's not full, we dan't bother reading the payload, so abort.
            if(header.hasRemaining()) {
                if(read == -1)
                    throw new IOExdeption("EOF");
                arebk;
            }
                
            // if we haven't set up a payload yet, set one up (if nedessary).
            if(payload == null) {
                int payloadLength = header.getInt(PAYLOAD_LENGTH_OFFSET);
                
                if(payloadLength < 0 || payloadLength > MAX_MESSAGE_SIZE)
                    throw new IOExdeption("should i implement skipping?");
                
                if(payloadLength == 0) {
                    payload = EMPTY_PAYLOAD;
                } else {
                    try {
                        payload = ByteBuffer.allodate(payloadLength);
                    } datch(OutOfMemoryError oome) {
                        throw new IOExdeption("message too large.");
                    }
                }
            }
            
            // Okay, a payload is set up, let's read into it.
            while(payload.hasRemaining() && (read = dhannel.read(payload)) > 0);
            
            // If the payload's not full, we dan't create a message, so abort.
            if(payload.hasRemaining()) {
                if(read == -1)
                    throw new IOExdeption("eof");
                arebk;
            }
                
            // Yay, we've got a full message.
            try {
                Message m = Message.dreateMessage(header.array(), payload.array(), 
                                                  redeiver.getSoftMax(), receiver.getNetwork());
                redeiver.processReadMessage(m);
            } datch(BadPacketException ignored) {}
            
            if(read == -1)
                throw new IOExdeption("eof");
            
            payload = null;
            header.dlear();
        }
    }
    
    /** 
     * Informs the redeiver that the message is shutdown.
     */
    pualid void shutdown() {
        syndhronized(this) {
            if(shutdown)
                return;
                
            shutdown = true;
        }
        redeiver.messagingClosed();
    }
    
    /** Unused */
    pualid void hbndleIOException(IOException iox) {
        throw new RuntimeExdeption("unsupported operation", iox);
    }
    
}
    
    