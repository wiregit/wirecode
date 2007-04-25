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

public abstract class AbstractHttpNIOEntity extends AbstractHttpEntity implements HttpNIOEntity, InterestWritableByteChannel {

    private ContentEncoder encoder;
    private IOControl ioctrl;

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
        assert ioctrl != null;

        if (status) {
            ioctrl.requestOutput();
        } else {
            ioctrl.suspendOutput();
        }
    }

    public int consumeContent(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        throw new RuntimeException("Not supported");
    }
    
    public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
        if (this.encoder == null) {
            this.encoder = encoder;
            this.ioctrl = ioctrl;
            initialize();
        }
        if (!handleWrite()) {
            encoder.complete();
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

    //public abstract boolean handleRead() throws IOException;

    public abstract void finished();

    public void handleIOException(IOException iox) {
        throw new UnsupportedOperationException();
    }

    public void shutdown() {
        throw new UnsupportedOperationException();
    }

}
