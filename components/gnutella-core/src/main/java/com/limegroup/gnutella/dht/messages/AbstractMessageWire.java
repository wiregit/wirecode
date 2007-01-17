package com.limegroup.gnutella.dht.messages;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.MessageID;
import org.limewire.mojito.routing.Contact;

import com.limegroup.gnutella.messages.Message;

/**
 * An abstract class that extends from Message and takes a 
 * DHTMessage as a delegate argument
 */
abstract class AbstractMessageWire<T extends DHTMessage> 
        extends Message implements DHTMessage {
    
    /** 
     * An empty GUID, it's never written to Network.
     * See overwritten write-methods for more info!
     */
    private static final byte[] GUID = new byte[16];
    
    /** Default TTL */
    private static final byte TTL = (byte)0x01;
    
    /** Default HOPS */
    private static final byte HOPS = (byte)0x00;
    
    protected final T delegate;
    
    AbstractMessageWire(T delegate) {
        super(GUID, (byte)DHTMessage.F_DHT_MESSAGE, TTL, HOPS, 0, N_UNKNOWN);
        this.delegate = delegate;
    }

    public void recordDrop() {
    }

    public Message stripExtendedPayload() {
        return this;
    }

    public Contact getContact() {
        return delegate.getContact();
    }

    public MessageID getMessageID() {
        return delegate.getMessageID();
    }

    public OpCode getOpCode() {
        return delegate.getOpCode();
    }
    
    @Override
    public void write(OutputStream out) throws IOException {
        delegate.write(out);
    }
    
    @Override
    public final void write(OutputStream out, byte[] buf) throws IOException {
        write(out);
    }

    @Override
    public final void writeQuickly(OutputStream out) throws IOException {
        write(out);
    }
    
    @Override
    protected final void writePayload(OutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public String toString() {
        return delegate.toString();
    }
}
