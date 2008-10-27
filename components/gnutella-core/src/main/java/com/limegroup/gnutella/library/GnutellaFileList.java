package com.limegroup.gnutella.library;

import java.io.File;

public interface GnutellaFileList extends FileList {

    /** Adds this FileList just for this session. */
    void addForSession(File file);

    /** Returns the size of all files within this list, in <b>bytes</b>. */
    long getNumBytes();

    /** Adds the FileDesc to this list. */
    boolean add(FileDesc fileDesc);
    
    /** Returns true if any files are shared through application special share folder. */
    boolean hasApplicationSharedFiles();
    
    /** Returns true if the given filename is shared through the application share. */
    boolean isFileApplicationShare(String filename);

}
