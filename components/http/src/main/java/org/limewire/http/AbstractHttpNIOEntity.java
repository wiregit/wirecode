package org.limewire.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.WriteObserver;

public abstract class AbstractHttpNIOEntity extends AbstractHttpEntity implements HttpNIOEntity, InterestWritableByteChannel {

    private ContentEncoder encoder;

    public InputStream getContent() throws IOException, IllegalStateException {
        throw new UnsupportedOperationException();
    }

    public long getContentLength() {
        return -1;
    }

    public boolean isRepeatable() {
        throw new UnsupportedOperationException();
    }

    public boolean isStreaming() {
        throw new UnsupportedOperationException();
    }

    public void writeTo(OutputStream outstream) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void interest(WriteObserver observer, boolean status) {
        assert observer == this;
        
        if (encoder == null) {
            return;
        }
        if (status) {
            encoder.requestOutput();
        } else {
            encoder.suspendOutput();
        }
    }

    public void produceContent(final ContentEncoder encoder) throws IOException {
        if (this.encoder == null) {
            this.encoder = encoder;
            initialize();
        }
        if (!handleWrite()) {
            encoder.complete();
            finished();
        }
    }
    
    public int write(ByteBuffer src) throws IOException {
        return encoder.write(src);
    }

    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }

    public abstract void initialize() throws IOException;
    
    public abstract boolean handleWrite() throws IOException;

    public abstract void finished() throws IOException;

    public void handleIOException(IOException iox) {
        throw new UnsupportedOperationException();
    }

    public void shutdown() {
        throw new UnsupportedOperationException();
    }

}
