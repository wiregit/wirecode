package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import org.apache.http.nio.reactor.IOEventDispatch;
import org.limewire.nio.BufferUtils;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;

/**
 * A read/write channel implementation that forwards all requests received from
 * HttpComponent to LimeWire's NIO layer.
 */
public class HttpChannel implements ByteChannel, ChannelReadObserver,
        ChannelWriter {

    private HttpIOSession session;

    private boolean closed;

    private IOEventDispatch eventDispatch;

    private InterestReadableByteChannel readSource;

    private InterestWritableByteChannel writeSource;

    private boolean writeInterest;

    private boolean readInterest;

    private ByteBuffer methodBuffer;

    public HttpChannel(HttpIOSession session, IOEventDispatch eventDispatch, String method) {
        this.session = session;
        this.eventDispatch = eventDispatch;
        if (method != null) {
            this.methodBuffer = ByteBuffer.wrap((method + " ").getBytes());
        }
    }

    public int read(ByteBuffer buffer) throws IOException {
        if (methodBuffer != null && methodBuffer.hasRemaining()) {
            // XXX need to read as much as we can
            int read = BufferUtils.transfer(methodBuffer, buffer, false);
            if (methodBuffer.hasRemaining()) {
                throw new RuntimeException();
            }
            return read + readSource.read(buffer);
        }
        return readSource.read(buffer);
    }

    public void close() throws IOException {
        shutdown();
    }

    public boolean isOpen() {
        return !closed;
    }

    public int write(ByteBuffer buffer) throws IOException {
        return writeSource.write(buffer);
    }

    public void handleRead() throws IOException {
        if (!readInterest) {
            return;
        }

        eventDispatch.inputReady(session);
    }

    public void handleIOException(IOException iox) {
        HttpIOReactor.LOG.error("Unexpected exception");
    }

    public void shutdown() {
        if (!closed) {
            closed = true;
            eventDispatch.disconnected(session);
        }
    }

    public InterestReadableByteChannel getReadChannel() {
        return readSource;
    }

    public void setReadChannel(InterestReadableByteChannel source) {
        this.readSource = source;
        this.readSource.interest(readInterest);
    }

    public synchronized InterestWritableByteChannel getWriteChannel() {
        return writeSource;
    }

    public synchronized void setWriteChannel(InterestWritableByteChannel channel) {
        this.writeSource = channel;
        this.writeSource.interest(this, writeInterest);
    }

    public boolean handleWrite() throws IOException {
        if (!writeInterest) {
            return false;
        }
        
        eventDispatch.outputReady(session);
        return writeInterest;
    }

    public void requestRead(boolean status) {
        this.readInterest = status;
        if (readSource != null) {
            readSource.interest(status);
        }
    }

    public void requestWrite(boolean status) {
        this.writeInterest = status;
        if (writeSource != null) {
            writeSource.interest(this, status);
        }
    }

}