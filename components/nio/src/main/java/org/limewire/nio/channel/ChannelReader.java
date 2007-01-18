package org.limewire.nio.channel;

/**
 * Allows arbitrary ReadableByteChannels to be set as the source
 * for reading from this object.
 */
public interface ChannelReader {
    
    /** Set the new source channel */
    void setReadChannel(InterestReadChannel newChannel);
    
    /** Gets the existing source channel. */
    InterestReadChannel getReadChannel();
}