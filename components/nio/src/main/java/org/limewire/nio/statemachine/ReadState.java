package org.limewire.nio.statemachine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;

public abstract class ReadState implements IOState {

    public final boolean isWriting() {
        return false;
    }

    public final boolean isReading() {
        return true;
    }

    public final boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        return processRead((ReadableByteChannel)channel, buffer);
    }
    
    /**
     * Reads data from the channel into the buffer. If this returns true, the state requires further processing.
     * This should be called repeatedly until it returns false, at which point the next state should
     * be used.
     */
    protected abstract boolean processRead(ReadableByteChannel channel, ByteBuffer buffer) throws IOException;

}
