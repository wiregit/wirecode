package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.limewire.io.IOUtils;

public class SwarmFileImpl implements SwarmFile {
    private final File file;

    private final long completeSize;

    private RandomAccessFile raFile;

    private FileChannel fileChannel;

    private long startByte = 0;

    private Object LOCK = new Object();

    public SwarmFileImpl(File file, long completeSize) {
        this.file = file;
        this.completeSize = completeSize;
    }

    /* (non-Javadoc)
     * @see org.limewire.swarm.file.SwarmFile#getFile()
     */
    public File getFile() {
        return file;
    }

    /* (non-Javadoc)
     * @see org.limewire.swarm.file.SwarmFile#getCompleteSize()
     */
    public long getFileSize() {
        return completeSize;
    }

    public void initialize() throws IOException {
        if (raFile == null) {
            raFile = new RandomAccessFile(file, "rw");
            fileChannel = raFile.getChannel();
        }
    }

    public void close() throws IOException {
        IOUtils.close(fileChannel);
        IOUtils.close(raFile);
        fileChannel = null;
        raFile = null;
    }

    /* (non-Javadoc)
     * @see org.limewire.swarm.file.SwarmFile#flush()
     */
    public void flush() throws IOException {
        synchronized (LOCK) {
            initialize();
            fileChannel.force(true);
        }
    }

    /* (non-Javadoc)
     * @see org.limewire.swarm.file.SwarmFile#read(java.nio.ByteBuffer, long)
     */
    public long read(ByteBuffer byteBuffer, long position) throws IOException {
        synchronized (LOCK) {
            initialize();
            long read = fileChannel.read(byteBuffer, position);
            return read;
        }
    }

    /* (non-Javadoc)
     * @see org.limewire.swarm.file.SwarmFile#write(java.nio.ByteBuffer, long)
     */
    public long write(ByteBuffer byteBuffer, long start) throws IOException {
        synchronized (LOCK) {
            initialize();
            long pendingBytes = getFileSize() - start;
            int oldLimit = byteBuffer.limit();
            int position = byteBuffer.position();
            int capacity = byteBuffer.capacity();
            long wrote = 0;
            try {
                if (pendingBytes < Integer.MAX_VALUE) {
                    int pending = (int) pendingBytes;
                    int limit = position + pending;
                    if (limit < capacity) {
                        byteBuffer.limit(limit);
                    }
                }
                wrote = fileChannel.write(byteBuffer, start);
            } finally {
                byteBuffer.limit(oldLimit);
            }
            return wrote;
        }
    }

    /* (non-Javadoc)
     * @see org.limewire.swarm.file.SwarmFile#getStartByte()
     */
    public long getStartByte() {
        return startByte;
    }

    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    /* (non-Javadoc)
     * @see org.limewire.swarm.file.SwarmFile#getEndByte()
     */
    public long getEndByte() {
        long endByte = getFileSize() > 0 ? getStartByte() + getFileSize() - 1 : 0;
        return endByte;
    }
}
