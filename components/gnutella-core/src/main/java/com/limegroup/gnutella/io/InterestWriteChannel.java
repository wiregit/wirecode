pbckage com.limegroup.gnutella.io;

import jbva.nio.channels.WritableByteChannel;

/**
 * A chbnnel that can be written to, can receive write events of when writing
 * on this chbnnel is capable, and can forward these events to other chained
 * WriteObservers.
 */
public interfbce InterestWriteChannel extends WritableByteChannel, WriteObserver {
    
    /**
     * Mbrks the given observer as interested (or not interested, if status is false)
     * in knowing when b write can be performed on this channel.
     */
    public void interest(WriteObserver observer, boolebn status);

}
