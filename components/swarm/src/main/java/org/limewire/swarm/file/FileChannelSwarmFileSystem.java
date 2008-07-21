package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.Range;
import org.limewire.io.IOUtils;
import org.limewire.swarm.SwarmFileSystem;

public class FileChannelSwarmFileSystem implements SwarmFileSystem {
    private final SwarmFile swarmFile;

    private RandomAccessFile raFile;

    private FileChannel fileChannel;

    private final Object LOCK = new Object();

    public FileChannelSwarmFileSystem(long completeSize, File file) {
        this.swarmFile = new SwarmFileImpl(file, completeSize);
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
        return swarmFile.getFileSize();
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
                raFile = new RandomAccessFile(swarmFile.getFile(), "rw");
                fileChannel = raFile.getChannel();
            }
        }
    }

    public SwarmFile getSwarmFile(long position) {
        return swarmFile;
    }

    public List<SwarmFile> getSwarmFilesInRange(Range range) {
        ArrayList<SwarmFile> files = new ArrayList<SwarmFile>();
        files.add(swarmFile);
        return files;
    }
}
