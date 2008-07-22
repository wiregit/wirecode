package org.limewire.swarm.file;

import java.io.File;

public class SwarmFileImpl implements SwarmFile {
    private final File file;

    private final long completeSize;



    private long startByte = 0;



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
