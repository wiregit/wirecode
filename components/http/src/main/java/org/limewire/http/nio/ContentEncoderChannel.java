package org.limewire.http.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.apache.http.nio.ContentEncoder;

/**
 * A {@link WritableByteChannel} that delegates to a {@link ContentEncoder}.
 */
public class ContentEncoderChannel implements WritableByteChannel {
    
    private final ContentEncoder contentEncoder;
    
    public ContentEncoderChannel(ContentEncoder contentEncoder) {
        this.contentEncoder = contentEncoder;
    }

    public int write(ByteBuffer src) throws IOException {
        return contentEncoder.write(src);
    }

    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }

}
