package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.Range;
import org.limewire.io.IOUtils;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmFileSystem;

public class SwarmFileSystemImpl implements SwarmFileSystem {
    private ArrayList<SwarmFile> swarmFiles = new ArrayList<SwarmFile>();

    private Map<SwarmFile, FileHandle> fileHandles = new HashMap<SwarmFile, FileHandle>();

    private long completeSize = 0;

    public SwarmFileSystemImpl() {

    }

    public SwarmFileSystemImpl(SwarmFileImpl swarmFile) {
        addSwarmFile(swarmFile);
    }

    public long getCompleteSize() {
        return completeSize;
    }

    public SwarmFile getSwarmFile(long position) {
        for (SwarmFile swarmFile : swarmFiles) {
            long startByte = swarmFile.getStartByte();
            long endByte = swarmFile.getEndByte();
            if (startByte <= position && endByte >= position) {
                return swarmFile;
            }
        }
        return null;

    }

    public List<SwarmFile> getSwarmFiles() {
        return swarmFiles;
    }

    public List<SwarmFile> getSwarmFilesInRange(Range range) {
        ArrayList<SwarmFile> filesRet = new ArrayList<SwarmFile>();

        long rangeStart = range.getLow();
        long rangeEnd = range.getHigh();

        for (SwarmFile swarmFile : swarmFiles) {
            long startByte = swarmFile.getStartByte();
            long endByte = swarmFile.getEndByte();
            if (startByte <= rangeEnd && endByte >= rangeStart) {
                filesRet.add(swarmFile);
            }
        }

        return filesRet;
    }

    public synchronized void addSwarmFile(SwarmFileImpl swarmFile) {
        swarmFiles.add(swarmFile);
        swarmFile.setStartByte(completeSize);
        completeSize += swarmFile.getFileSize();
    }

    public long write(ByteBuffer byteBuffer, long start) throws IOException {
        initialize();
        long currentPosition = start;
        long wroteTotal = 0;

        while (byteBuffer.position() < byteBuffer.limit()) {
            SwarmFile swarmFile = getSwarmFile(currentPosition);
            long writeStart = currentPosition - swarmFile.getStartByte();
            long wrote = getFileHandle(swarmFile).write(byteBuffer, writeStart);
            currentPosition += wrote;
            wroteTotal += wrote;
        }
        return wroteTotal;
    }

    public long read(ByteBuffer byteBuffer, long position) throws IOException {
        initialize();
        SwarmFile swarmFile = getSwarmFile(position);
        long read = getFileHandle(swarmFile).read(byteBuffer, position);
        return read;
    }

    public void close() throws IOException {
        for (SwarmFile swarmFile : swarmFiles) {
            FileHandle fileHandle = getFileHandle(swarmFile);
            fileHandle.close();
        }
    }

    public synchronized void initialize() throws IOException {

    }

    private synchronized FileHandle getFileHandle(SwarmFile swarmFile) {
        FileHandle fileHandle = fileHandles.get(swarmFile);
        if (fileHandle == null) {
            fileHandle = new FileHandle(swarmFile);
            fileHandles.put(swarmFile, fileHandle);
        }
        return fileHandle;
    }

    private class FileHandle {
        private SwarmFile swarmFile = null;

        private Object LOCK = new Object();

        private RandomAccessFile raFile;

        private FileChannel fileChannel;

        private File file = null;

        public FileHandle(SwarmFile swarmFile) {
            this.swarmFile = swarmFile;
            this.file = swarmFile.getFile();
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


        public void flush() throws IOException {
            synchronized (LOCK) {
                initialize();
                fileChannel.force(true);
            }
        }

        public long read(ByteBuffer byteBuffer, long position) throws IOException {
            synchronized (LOCK) {
                initialize();
                long read = fileChannel.read(byteBuffer, position);
                return read;
            }
        }

        public long write(ByteBuffer byteBuffer, long start) throws IOException {
            synchronized (LOCK) {
                initialize();
                long pendingBytes = swarmFile.getFileSize() - start;
                int oldLimit = byteBuffer.limit();
                int position = byteBuffer.position();
                long wrote = 0;
                try {
                    if (pendingBytes < Integer.MAX_VALUE) {
                        int pending = (int) pendingBytes;
                        int limit = position + pending;
                        if (limit < oldLimit) {
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
    }

}
