package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.util.BufferUtils;

public abstract class AbstractChannelInterestReader implements ChannelReadObserver, InterestScatteringByteChannel {
    protected ByteBuffer buffer;
    protected InterestReadableByteChannel source;
    protected boolean shutdown;
    
    public AbstractChannelInterestReader(int bufferSize) {
        buffer = ByteBuffer.allocate(bufferSize);
    }
    
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

    public InterestReadableByteChannel getReadChannel() {
        return source;
    }

    public void setReadChannel(InterestReadableByteChannel newChannel) {
        this.source = newChannel;
    }

    public void interestRead(boolean status) {
        source.interestRead(status);
    }

    public void close() throws IOException {
        source.close();
    }

    public boolean isOpen() {
        return source.isOpen();
    }

    public void handleIOException(IOException iox) {}
    
}
