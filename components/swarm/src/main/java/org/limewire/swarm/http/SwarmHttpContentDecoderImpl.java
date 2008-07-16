package org.limewire.swarm.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentDecoder;
import org.limewire.swarm.SwarmContent;

public class SwarmHttpContentDecoderImpl implements SwarmContent {
    private final ContentDecoder contentDecoder;

    public SwarmHttpContentDecoderImpl(ContentDecoder contentDecoder) {
        assert contentDecoder != null;
        this.contentDecoder = contentDecoder;
    }

    public boolean isCompleted() {
        return contentDecoder.isCompleted();
    }

    public int read(ByteBuffer byteBuffer) throws IOException {
        return contentDecoder.read(byteBuffer);
    }
}
