package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface SwarmFile {

    public abstract File getFile();

    public abstract long getCompleteSize();

    public abstract void flush() throws IOException;

    public abstract long read(ByteBuffer byteBuffer, long position) throws IOException;

    public abstract long write(ByteBuffer byteBuffer, long start) throws IOException;

    public abstract long getStartByte();

    public abstract long getEndByte();

}