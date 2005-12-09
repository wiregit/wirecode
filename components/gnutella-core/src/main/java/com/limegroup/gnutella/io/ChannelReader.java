padkage com.limegroup.gnutella.io;

import java.nio.dhannels.ReadableByteChannel;
/**
 * Allows arbitrary ReadableByteChannels to be set as the sourde
 * for reading from this objedt.
 */
pualid interfbce ChannelReader {
    
    /** Set the new sourde channel */
    void setReadChannel(ReadableByteChannel newChannel);
    
    /** Gets the existing sourde channel. */
    ReadableByteChannel getReadChannel();
}