package org.limewire.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.WriteObserver;

/**
 * Provides a default implementation of an event based HTTP entity that
 * implements {@link InterestWritableByteChannel}. Inherited methods from
 * {@HttpEntity} for streaming throw {@link UnsupportedOperationException}.
 * <p>
 * NOTE: This class only supports writing, reading has not been implemented.
 */
public abstract class AbstractHttpNIOEntity extends AbstractHttpEntity
        implements HttpNIOEntity, InterestWritableByteChannel {

    private ContentEncoder encoder;

    private IOControl ioctrl;

    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public abstract long getContentLength();
    
    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public boolean isRepeatable() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public boolean isStreaming() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public void writeTo(OutputStream outstream) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     */
    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets write interest to <code>status</code>.
     * <p>
     * Note: Ignores <code>observer</code>
     */
    public void interestWrite(WriteObserver observer, boolean status) {       
        assert observer == this;
        assert ioctrl != null;

        if (status) {
            ioctrl.requestOutput();
        } else {
            ioctrl.suspendOutput();
        }
    }

    public int consumeContent(ContentDecoder decoder, IOControl ioctrl)
            throws IOException {
        throw new RuntimeException("Not supported");
    }

    public void produceContent(ContentEncoder encoder, IOControl ioctrl)
            throws IOException {
        if (this.encoder == null) {
            this.encoder = encoder;
            this.ioctrl = ioctrl;
            
            initialize();
        }
        if (!handleWrite()) {
            encoder.complete();
        }
    }

    /**
     * Writes data from <code>src</code> to encoder.
     * 
     * @return number of bytes written
     */
    public int write(ByteBuffer src) throws IOException {
        return encoder.write(src);
    }

    /**
     * Sub-classes need to implement this and invoke {@link #write(ByteBuffer)}
     * to transmit data.
     * 
     * @throws IOException indicates an I/O error which will abort the
     *         connection
     * @return true, if more data is expected; false, if the transfer is complete  
     */
    public abstract boolean handleWrite() throws IOException;

    /**
     * Invoked before the first call to {@link #handleWrite()}.
     * 
     * @throws IOException indicates an I/O error which will abort the
     *         connection
     */
    public abstract void initialize() throws IOException;

    /**
     * Invoked after transfer has completed. This is true if either
     * {@link #handleWrite()} returns false or {@link #handleWrite()} or
     * {@link #initialize()} thrown an exception.
     */
    public abstract void finished();

    /**
     * Never invoked, throws <code>UnsupportedOperationException</code>.
     */
    public void handleIOException(IOException iox) {
        throw new UnsupportedOperationException();
    }

    /**
     * Never invoked, throws <code>UnsupportedOperationException</code>.
     */
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    /** 
     * Returns false.
     */
    public boolean hasBufferedOutput() {
        return false;
    }
    
}
