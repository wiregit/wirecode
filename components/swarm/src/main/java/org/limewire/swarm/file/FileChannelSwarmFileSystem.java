package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.limewire.io.IOUtils;
import org.limewire.swarm.SwarmFileSystem;

public class FileChannelSwarmFileSystem implements SwarmFileSystem {
    private final File file;

    private RandomAccessFile raFile;

    private FileChannel fileChannel;

    private final Object LOCK = new Object();

    private final long completeSize;

    public FileChannelSwarmFileSystem(long completeSize, File file) {
        this.file = file;
        this.completeSize = completeSize;
    }

    public long write(ByteBuffer byteBuffer, long start) throws IOException {
        synchronized (LOCK) {
            initialize();
            long written = fileChannel.write(byteBuffer, start);
            return written;
        }
    }

    public long read(ByteBuffer byteBuffer, long position) throws IOException {
        synchronized (LOCK) {
            initialize();
            long read = fileChannel.read(byteBuffer, position);
            return read;
        }
    }

    public long getCompleteSize() {
        return completeSize;
    }

    public void close() throws IOException {
        synchronized (LOCK) {
            if (raFile != null) {
                IOUtils.close(fileChannel);
                IOUtils.close(raFile);
            }
        }
    }

    public void initialize() throws IOException {
        synchronized (LOCK) {
            if (raFile == null) {
                raFile = new RandomAccessFile(file, "rw");
                fileChannel = raFile.getChannel();
            }
        }
    }
}
