package com.limegroup.gnutella.rudp.messages;

import java.nio.ByteBuffer;

import org.limewire.rudp.messages.AckMessage;
import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.FinMessage;
import org.limewire.rudp.messages.KeepAliveMessage;
import org.limewire.rudp.messages.MessageFactory;
import org.limewire.rudp.messages.MessageFormatException;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.rudp.messages.impl.SynMessageImpl;

public class MessageFactoryWire implements MessageFactory {
    private final MessageFactory delegate;

    public MessageFactoryWire(MessageFactory delegate) {
        if (delegate == null) {
            throw new NullPointerException("Delegate is null");
        } else if (delegate instanceof MessageFactoryWire) {
            throw new IllegalArgumentException("Recursive delegation");
        }
        
        this.delegate = delegate;
    }

    public RUDPMessage createMessage(ByteBuffer... data) throws MessageFormatException {
        RUDPMessage msg = delegate.createMessage(data);
        
        if (msg instanceof AckMessage) {
            return new AckMessageWireImpl((AckMessage)msg);
        } else if (msg instanceof DataMessage) {
            return new DataMessageWireImpl((DataMessage)msg);
        } else if (msg instanceof FinMessage) {
            return new FinMessageWireImpl((FinMessage)msg);
        } else if (msg instanceof KeepAliveMessage) {
            return new KeepAliveMessageWireImpl((KeepAliveMessage)msg);
        } else if (msg instanceof SynMessageImpl) {
            return new SynMessageWireImpl((SynMessageImpl)msg);
        }
        
        throw new IllegalArgumentException(msg.getClass() + " is unhandled");
    }

    public AckMessage createAckMessage(byte connectionID, long sequenceNumber, long windowStart, int windowSpace) {
        return new AckMessageWireImpl(
                delegate.createAckMessage(connectionID, sequenceNumber, windowStart, windowSpace));
    }

    public DataMessage createDataMessage(byte connectionID, long sequenceNumber, ByteBuffer chunk) {
        return new DataMessageWireImpl(
                delegate.createDataMessage(connectionID, sequenceNumber, chunk));
    }

    public FinMessage createFinMessage(byte connectionID, long sequenceNumber, byte reasonCode) {
        return new FinMessageWireImpl(
                delegate.createFinMessage(connectionID, sequenceNumber, reasonCode));
    }

    public KeepAliveMessage createKeepAliveMessage(byte connectionID, long windowStart, int windowSpace) {
        return new KeepAliveMessageWireImpl(
                delegate.createKeepAliveMessage(connectionID, windowStart, windowSpace));
    }

    public SynMessage createSynMessage(byte connectionID) {
        return new SynMessageWireImpl(
                delegate.createSynMessage(connectionID));
    }

    public SynMessage createSynMessage(byte connectionID, byte theirConnectionID) {
        return new SynMessageWireImpl(
                delegate.createSynMessage(connectionID, theirConnectionID));
    }
}
