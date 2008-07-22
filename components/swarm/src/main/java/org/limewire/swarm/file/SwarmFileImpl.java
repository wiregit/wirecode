package org.limewire.swarm.file;

import java.io.File;

public class SwarmFileImpl implements SwarmFile {
    private final File file;

    private final long completeSize;

    private long startByte = 0;

    private String path;

    public SwarmFileImpl(File file, String path, long completeSize) {
        this.file = file;
        this.completeSize = completeSize;
        this.path = path;
        if (this.path == null) {
            this.path = file.getName();
        }
    }

    public SwarmFileImpl(File file, long completeSize) {
        this(file, file.getName(), completeSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getFile()
     */
    public File getFile() {
        return file;
    }

    public String getPath() {
        return path;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getCompleteSize()
     */
    public long getFileSize() {
        return completeSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getStartByte()
     */
    public long getStartByte() {
        return startByte;
    }

    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getEndByte()
     */
    public long getEndByte() {
        long endByte = getFileSize() > 0 ? getStartByte() + getFileSize() - 1 : 0;
        return endByte;
    }
}
