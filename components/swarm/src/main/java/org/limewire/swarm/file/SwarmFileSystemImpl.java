package org.limewire.swarm.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.Range;
import org.limewire.swarm.SwarmFileSystem;

public class SwarmFileSystemImpl implements SwarmFileSystem {
    private ArrayList<SwarmFileImpl> files = new ArrayList<SwarmFileImpl>();

    private long completeSize = 0;

    public SwarmFileSystemImpl() {

    }

    public SwarmFileSystemImpl(SwarmFileImpl swarmFile) {
        add(swarmFile);
    }

    public long getCompleteSize() {
        return completeSize;
    }

    public SwarmFile getSwarmFile(long position) {
        for (SwarmFile swarmFile : files) {
            long startByte = swarmFile.getStartByte();
            long endByte = swarmFile.getEndByte();
            if (startByte <= position && endByte >= position) {
                return swarmFile;
            }
        }
        return null;

    }

    public List<SwarmFile> getSwarmFilesInRange(Range range) {
        ArrayList<SwarmFile> files = new ArrayList<SwarmFile>();
        
        long rangeStart = range.getLow();
        long rangeEnd = range.getHigh();
        
        for (SwarmFile swarmFile : files) {
            long startByte = swarmFile.getStartByte();
            long endByte = swarmFile.getEndByte();
            if (startByte <= rangeEnd && endByte >= rangeStart) {
                files.add(swarmFile);
            }
        }

        return files;
    }

    public synchronized void add(SwarmFileImpl swarmFile) {
        files.add(swarmFile);
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
            long wrote = swarmFile.write(byteBuffer, writeStart);
            currentPosition += wrote;
            wroteTotal += wrote;
        }
        return wroteTotal;
    }

    public long read(ByteBuffer byteBuffer, long position) throws IOException {
        initialize();
        SwarmFile swarmFile = getSwarmFile(position);
        long read = swarmFile.read(byteBuffer, position);
        return read;
    }

    public void close() throws IOException {
        for (SwarmFileImpl swarmFile : files) {
            swarmFile.close();
        }
    }

    public void initialize() throws IOException {

    }

}
