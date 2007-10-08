package org.limewire.rudp.messages;

import java.nio.ByteBuffer;

/**
 * Creates every kind of <code>RUDPMessage</code>.
 */
public interface RUDPMessageFactory {

    /** Deserializes a message as read from the network. */
    public RUDPMessage createMessage(ByteBuffer... data) throws MessageFormatException;
    
    /** Constructs a new AckMessage. */
    public AckMessage createAckMessage(byte connectionID, long sequenceNumber, long windowStart, int windowSpace);
    
    /** Creates a new DataMessage. */
    public DataMessage createDataMessage(byte connectionID, long sequenceNumber, ByteBuffer chunk);
    
    /** Creates a new FinMessage */
    public FinMessage createFinMessage(byte connectionID, long sequenceNumber, byte reasonCode);
    
    /** Creates a new KeepAliveMessage. */
    public KeepAliveMessage createKeepAliveMessage(byte connectionID, long windowStart, int windowSpace);
    
    /** Creates a new SynMessage with just their connection id. */
    public SynMessage createSynMessage(byte connectionID);
    
    /** Creates a new SynMessage with their & our connection id. */
    public SynMessage createSynMessage(byte connectionID, byte theirConnectionID);
}
