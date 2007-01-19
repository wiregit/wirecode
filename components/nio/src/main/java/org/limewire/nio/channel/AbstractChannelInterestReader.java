package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.nio.BufferUtils;

public abstract class AbstractChannelInterestReader implements ChannelReadObserver, InterestScatteringByteChannel {
    protected ByteBuffer buffer;
    protected InterestReadChannel source;
    protected boolean shutdown;
    
    public AbstractChannelInterestReader() {
        buffer = ByteBuffer.allocate(getBufferSize());
    }
    
    protected abstract int getBufferSize();

    public int read(ByteBuffer dst) {
        return BufferUtils.transfer(buffer, dst);
    }
    
    public long read(ByteBuffer [] dst) {
    	return read(dst, 0, dst.length);
    }
    
    public long read(ByteBuffer [] dst, int offset, int length) {
    	return BufferUtils.transfer(buffer, dst, offset, length, true);
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
