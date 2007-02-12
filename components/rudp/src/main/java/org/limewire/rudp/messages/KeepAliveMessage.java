package org.limewire.rudp.messages;

public interface KeepAliveMessage {

    /**
     *  The windowStart is equivalent to the lowest unreceived sequenceNumber
     *  coming from the receiving end of the connection.  It is saying, I have 
     *  received everything up to one minus this. (Note: it rolls)
     */
    public abstract long getWindowStart();

    /**
     *  Extend the windowStart of incoming messages with the full 8 bytes
     *  of state
     */
    public abstract void extendWindowStart(long wStart);

    /**
     *  The windowSpace is a measure of how much more data the receiver can 
     *  receive within its buffer.  This number will go to zero if the 
     *  application on the receiving side is reading data slowly.  If it goes 
     *  to zero then the sender should stop sending.
     */
    public abstract int getWindowSpace();

}