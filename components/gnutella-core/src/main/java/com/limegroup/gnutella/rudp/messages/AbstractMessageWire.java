package com.limegroup.gnutella.rudp.messages;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.rudp.messages.RUDPMessage;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.DataUtils;

/**
 * An abstract class that extends from Message and takes a 
 * DHTMessage as a delegate argument
 */
abstract class AbstractMessageWire<T extends RUDPMessage> 
        extends Message implements RUDPMessage {
    
    /** 
     * An empty GUID, it's never written to Network.
     * See overwritten write-methods for more info!
     */
    private static final byte[] GUID = DataUtils.EMPTY_GUID;
    
    /** Default TTL */
    private static final byte TTL = (byte)0x01;
    
    /** Default HOPS */
    private static final byte HOPS = (byte)0x00;
    
    /** The message this is wrapping. */
    protected final T delegate;
    
    AbstractMessageWire(T delegate) {
        super(GUID, RUDPMessage.F_RUDP_MESSAGE, TTL, HOPS, 0, N_UDP);
        this.delegate = delegate;
    }

    public void recordDrop() {
    }

    public Message stripExtendedPayload() {
        return this;
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

    public void extendSequenceNumber(long seqNo) {
        delegate.extendSequenceNumber(seqNo);
    }

    public byte getConnectionID() {
        return delegate.getConnectionID();
    }

    public int getDataLength() {
        return delegate.getDataLength();
    }

    public long getSequenceNumber() {
        return delegate.getSequenceNumber();
    }
}
