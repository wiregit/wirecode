package org.limewire.nio.channel;

import java.nio.channels.ReadableByteChannel;

public interface InterestReadableByteChannel extends ReadableByteChannel {

    /** 
     * Allows this ReadableByteChannel to be told that someone is no
     * longer interested in reading from it.
     */
    public void interest(boolean status);
    
}
