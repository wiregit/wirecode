package org.limewire.core.api.library;

import java.io.File;

/**
 * A File that is displayed in a library
 */
public interface FileItem {

    File getFile();
    
    String getName();
    
    long getSize();
    
    long getCreationTime();
    
    long getLastModifiedTime();
    
    int getNumHits();
    
    int getNumUploads();
}
