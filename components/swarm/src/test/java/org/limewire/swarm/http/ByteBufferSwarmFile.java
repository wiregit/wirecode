package org.limewire.swarm.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentDecoder;
import org.limewire.swarm.file.SwarmFile;

public class ByteBufferSwarmFile implements SwarmFile {
    
    private final ByteBuffer buffer;
    
    public ByteBufferSwarmFile(int size) {
        buffer = ByteBuffer.allocate(size);
    }

    public void finish() {
    }

    public void initialize() throws IOException {
    }

    public long transferFrom(ContentDecoder decoder, long start) throws IOException {
        buffer.position((int)start);
        return decoder.read(buffer);
    }

    public long transferTo(ByteBuffer buffer, long start) throws IOException {
        throw new UnsupportedOperationException();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
    
}
