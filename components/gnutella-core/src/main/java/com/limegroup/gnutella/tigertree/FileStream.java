package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public interface FileStream {
    
    /**
     * See {@link FileChannel#read(ByteBuffer, long)}, except this always read
     * the whole range of the buffer.
     * 
     * @throws IOException
     */
    void read(ByteBuffer buffer, long position) throws IOException;

}
