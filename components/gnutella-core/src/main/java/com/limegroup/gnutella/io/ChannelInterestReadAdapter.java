package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class ChannelInterestReadAdapter implements ChannelReadObserver, InterestReadChannel {
    protected ByteBuffer buffer;
    protected InterestReadChannel source;
    protected boolean shutdown;
    
    public ChannelInterestReadAdapter() {
        buffer = ByteBuffer.allocate(getBufferSize());
    }
    
    protected abstract int getBufferSize();

    public int read(ByteBuffer dst) {
        return BufferUtils.transfer(buffer, dst);
    }

    public void shutdown() {
        shutdown = true;
    }

    public InterestReadChannel getReadChannel() {
        return source;
    }

    public void setReadChannel(InterestReadChannel newChannel) {
        this.source = newChannel;
    }

    public void interest(boolean status) {
        source.interest(status);
    }

    public void close() throws IOException {
        source.close();
    }

    public boolean isOpen() {
        return source.isOpen();
    }

    public void handleIOException(IOException iox) {}
    
}
