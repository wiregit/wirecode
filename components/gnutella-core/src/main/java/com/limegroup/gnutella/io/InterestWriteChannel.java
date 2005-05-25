package com.limegroup.gnutella.io;

import java.nio.channels.WritableByteChannel;

/**
 * A channel that can be written to, can receive write events of when writing
 * on this channel is capable, and can forward these events to other chained
 * WriteObservers.
 */
public interface InterestWriteChannel extends WritableByteChannel, WriteObserver {
    
    /**
     * Marks the given observer as interested (or not interested, if status is false)
     * in knowing when a write can be performed on this channel.
     */
    public void interest(WriteObserver observer, boolean status);

}