package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.nio.reactor.IOEventDispatch;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.util.BufferUtils;

/**
 * A read/write channel implementation that forwards all requests received from
 * LimeWire's NIO layer to HttpCore's {@link IOEventDispatch}. 
 */
public class HttpChannel implements ByteChannel, ChannelReadObserver,
        ChannelWriter {

    private final HttpIOSession session;

    private final IOEventDispatch eventDispatch;

    private AtomicBoolean closed = new AtomicBoolean(false);

    private InterestReadableByteChannel readSource;

    private InterestWritableByteChannel writeSource;

    private boolean writeInterest;

    private boolean readInterest;

    private ByteBuffer methodBuffer;

    /**
     * Constructs a channel optionally pushing back a string that will be read
     * first. LimeWire's acceptor eats the first word of a connection to
     * determine the type. If a non-null value is passed as <code>method</code>
     * this word can be pushed back into the channel and will be the first
     * data returned by {@link #read(ByteBuffer)}.
     * 
     * @param session the IO session
     * @param eventDispatch the IO event dispatcher that 
     * @param method if != null, the content will be pushed back into the
     *        channel
     */
    public HttpChannel(HttpIOSession session, IOEventDispatch eventDispatch, String method) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        if (eventDispatch == null) {
            throw new IllegalArgumentException("eventDispatch must not be null");
        }

        this.session = session;
        this.eventDispatch = eventDispatch;
        if (method != null) {
            this.methodBuffer = ByteBuffer.wrap(method.getBytes());
        }
    }

    /**
     * Constructs a channel that does not push back a method.
     * 
     * @see  #HttpChannel(HttpIOSession, IOEventDispatch)
     */
    public HttpChannel(HttpIOSession session, IOEventDispatch eventDispatch) {
        this(session, eventDispatch, null);
    }
    
    public int read(ByteBuffer buffer) throws IOException {
        if (methodBuffer != null) {
            int read = BufferUtils.transfer(methodBuffer, buffer, false);
            if (methodBuffer.hasRemaining()) {
                return read;
            }
            methodBuffer = null;
            return read + readSource.read(buffer);
        }
        return readSource.read(buffer);
    }

    public void close() throws IOException {
        shutdown();
    }

    public boolean isOpen() {
        return !closed.get();
    }

    public int write(ByteBuffer buffer) throws IOException {
        return writeSource.write(buffer);
    }

    public void handleRead() throws IOException {
        if (!readInterest) {
            // HttpIOSession turns off read interest before switching channels
            // using AbstractNBSocket#setReadObserver(), do not delegate to http
            // core in this case
            return;
        }

        eventDispatch.inputReady(session);
    }

    public void handleIOException(IOException iox) {
        HttpIOReactor.LOG.error("Unexpected exception");
    }

    public void shutdown() {
        if (!closed.getAndSet(true)) {
            NIODispatcher.instance().invokeLater(new Runnable() {
                public void run() {
                    eventDispatch.disconnected(session);
                }               
            });
        }
    }

    public InterestReadableByteChannel getReadChannel() {
        return readSource;
    }

    public void setReadChannel(InterestReadableByteChannel source) {
        this.readSource = source;
        if (this.readSource != null) {
            this.readSource.interestRead(readInterest);
        }
    }

    public synchronized InterestWritableByteChannel getWriteChannel() {
        return writeSource;
    }

    public synchronized void setWriteChannel(InterestWritableByteChannel channel) {
        this.writeSource = channel;
        if (this.writeSource != null) {
            this.writeSource.interestWrite(this, writeInterest);
        }
    }

    public boolean handleWrite() throws IOException {
        if (!writeInterest) {
            return false;
        }

        eventDispatch.outputReady(session);
        
        return session.hasBufferedOutput();
    }

    public void requestRead(boolean status) {
        this.readInterest = status;
        if (readSource != null) {
            readSource.interestRead(status);
        }
    }

    public void requestWrite(boolean status) {
        this.writeInterest = status;
        if (writeSource != null) {
            writeSource.interestWrite(this, status);
        }
    }
    
    public boolean isWriteInterest() {
        return writeInterest;
    }
    
    public boolean isReadInterest() {
        return readInterest;
    }

}