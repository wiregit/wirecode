package org.limewire.rudp.messages;

public interface RUDPMessage {

    // The version number of the protocol to allow for future improvements
    public static final int PROTOCOL_VERSION_NUMBER = 0;

    /**
     *  Return the messages connectionID identifier.
     */
    public abstract byte getConnectionID();

    /**
     *  Return the messages sequence number
     */
    public abstract long getSequenceNumber();

    /**
     *  Extend the sequence number of incoming messages with the full 8 bytes
     *  of state
     */
    public abstract void extendSequenceNumber(long seqNo);

    /** 
     *  Return the length of data stored in this message.
     */
    public abstract int getDataLength();

}