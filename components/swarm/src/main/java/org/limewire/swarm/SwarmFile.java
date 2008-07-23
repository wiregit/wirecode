package org.limewire.swarm;

import java.io.File;

public interface SwarmFile {

    /**
     * Returns the local file backing this file.
     * 
     * @return
     */
    public abstract File getFile();

    /**
     * The total file size of the complete file.
     * 
     * @return
     */
    public abstract long getFileSize();

    /**
     * The start byte for this file, in whatever file system it is represented
     * by.
     * 
     * @return
     */
    public abstract long getStartByte();

    /**
     * The end byte for this file, in whatever file system it is represented by.
     * 
     * @return
     */
    public abstract long getEndByte();

    /**
     * Returns the base path for this file. Can be used for remote and local
     * repositories.
     * 
     * @return
     */
    public abstract String getPath();

}