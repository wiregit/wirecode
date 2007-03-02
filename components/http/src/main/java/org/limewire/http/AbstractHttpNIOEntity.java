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
        return 0;
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
        this.encoder = encoder;
        handleWrite();
    }
    
    public int write(ByteBuffer src) throws IOException {
        return encoder.write(src);
    }

    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    public abstract boolean handleWrite() throws IOException;

    public void handleIOException(IOException iox) {
        // TODO Auto-generated method stub
        
    }

    public void shutdown() {
        throw new UnsupportedOperationException();
    }

}
