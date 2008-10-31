package org.limewire.core.api.library;

import org.limewire.core.api.URN;

/**
 * A File that is displayed in a library
 */
public interface FileItem extends PropertiableFile {
    
    String getName();
    
    long getSize();

    long getCreationTime();

    long getLastModifiedTime();

    int getNumHits();

    int getNumUploads();

    URN getUrn();
    
}
