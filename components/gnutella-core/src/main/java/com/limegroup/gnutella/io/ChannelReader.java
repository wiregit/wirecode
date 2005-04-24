package com.limegroup.gnutella.io;

/**
 * Marks the class as using a channel and allowing that channel
 * to change at some point in time.
 */
public interface ChannelReader {
    public void setReadChannel(java.nio.channels.ReadableByteChannel newChannel);
}