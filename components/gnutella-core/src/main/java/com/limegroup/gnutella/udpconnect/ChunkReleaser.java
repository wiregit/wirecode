package com.limegroup.gnutella.udpconnect;

import java.nio.ByteBuffer;

public interface ChunkReleaser {

    public void releaseChunk(ByteBuffer chunk);
}
