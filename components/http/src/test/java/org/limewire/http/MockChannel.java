package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.util.BufferUtils;

public class MockChannel implements InterestReadableByteChannel {

    private ByteBuffer source;

    private boolean closed;

    public MockChannel(ByteBuffer source) {
        this.source = source;
    }

    public MockChannel(byte[] data) {
        this.source = ByteBuffer.wrap(data);
    }

    public MockChannel(String data) {
        this(data.getBytes());
    }

    public void interestRead(boolean status) {
    }

    public int read(ByteBuffer dst) throws IOException {
        return BufferUtils.transfer(source, dst, false);
    }

    public void close() throws IOException {
        closed = true;
    }

    public boolean isOpen() {
        return closed;
    }

}
