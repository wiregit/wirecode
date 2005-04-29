package com.limegroup.gnutella.io;

import java.nio.channels.ReadableByteChannel;
/**
 * Allows arbitrary ReadableByteChannels to be set as the source
 * for reading from this object.
 */
public interface ChannelReader {
    
    /** Set the new source channel */
    void setReadChannel(ReadableByteChannel newChannel);
    
    /** Gets the existing source channel. */
    ReadableByteChannel getReadChannel();
}