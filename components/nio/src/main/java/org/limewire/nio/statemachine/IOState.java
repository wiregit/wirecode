package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

/** Outlines all the states an asynchronous handshake can be in. */
public interface IOState {
   
    /** Determines if this HandshakeState is for writing. */
    boolean isWriting();
    
    /** Determines if this HandshakeState is for reading. */
    boolean isReading();
    
    /**
     * Processes this HandshakeState.  If this returns true, the state requires further processing.
     * This should be called repeatedly until it returns false, at which point the next state should
     * be used.
     * 
     * The given Channel must be a ReadableByteChannel if it is for reading or a 
     * WritableByteChannel if it is for writing.
     * 
     * The given ByteBuffer should be used as scratch space for reading.
     */
    boolean process(Channel channel, ByteBuffer buffer) throws IOException;
    
    /**
     * Returns the amount of data that has been processed by this IOState.
     * This operation is optional.  It should return -1 if unsupported.
     * 
     * @return
     */
    long getAmountProcessed();
}
