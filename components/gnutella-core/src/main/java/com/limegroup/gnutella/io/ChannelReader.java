pbckage com.limegroup.gnutella.io;

import jbva.nio.channels.ReadableByteChannel;
/**
 * Allows brbitrary ReadableByteChannels to be set as the source
 * for rebding from this object.
 */
public interfbce ChannelReader {
    
    /** Set the new source chbnnel */
    void setRebdChannel(ReadableByteChannel newChannel);
    
    /** Gets the existing source chbnnel. */
    RebdableByteChannel getReadChannel();
}
