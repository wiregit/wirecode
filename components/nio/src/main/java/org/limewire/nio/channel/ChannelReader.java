package org.limewire.nio.channel;

/**
 * Allows arbitrary ReadableByteChannels to be set as the source
 * for reading from this object.
 */
public interface ChannelReader {
    
    /** Set the new source channel */
    void setReadChannel(InterestReadableByteChannel newChannel);
    
    /** Gets the existing source channel. */
    InterestReadableByteChannel getReadChannel();
}