package org.limewire.swarm.file;

import java.io.File;

public interface SwarmFile {

    public abstract File getFile();

    public abstract long getFileSize();

    public abstract long getStartByte();

    public abstract long getEndByte();
    
    public abstract String getPath();

}