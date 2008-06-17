package org.limewire.swarm.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ProducingNHttpEntity;

public class ByteBufferEntity
    extends AbstractHttpEntity implements ProducingNHttpEntity {

    protected final ByteBuffer buffer;
    protected final long contentLength;

    public ByteBufferEntity(ByteBuffer buffer) {
        this.buffer = buffer;
        this.contentLength = buffer.remaining();
    }

    public void finish() {
        buffer.rewind();
    }

    public void produceContent(ContentEncoder encoder, IOControl ioctrl)
            throws IOException {
        encoder.write(buffer);
        if(!buffer.hasRemaining())
            encoder.complete();
    }

    public long getContentLength() {
        return contentLength;
    }

    public boolean isRepeatable() {
        return true;
    }

    public boolean isStreaming() {
        return false;
    }

    public InputStream getContent() {
        throw new UnsupportedOperationException();
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        throw new UnsupportedOperationException();
    }

}
