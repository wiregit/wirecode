package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import com.limegroup.gnutella.tigertree.FileStream;

public class RandomAccessFileStream implements FileStream {
    
    private final RandomAccessFile raf;

    public RandomAccessFileStream(RandomAccessFile fos) {
        this.raf = fos;
    }

    public void read(ByteBuffer buffer, long position) throws IOException {
        synchronized(raf) {
            raf.seek(position);
            raf.readFully(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        }
        buffer.position(buffer.limit());
    }

}
