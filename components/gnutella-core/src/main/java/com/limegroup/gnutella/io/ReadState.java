package com.limegroup.gnutella.io;

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
    
    protected abstract boolean processRead(ReadableByteChannel channel, ByteBuffer buffer) throws IOException;

}
