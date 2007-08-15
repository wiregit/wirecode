package org.limewire.rudp;

import java.nio.ByteBuffer;

public interface ChunkReleaser {

    public void releaseChunk(ByteBuffer chunk);
}
