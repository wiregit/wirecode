package org.limewire.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface SwarmContent {
    public int read(ByteBuffer byteBuffer) throws IOException;
}
