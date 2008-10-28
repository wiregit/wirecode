package org.limewire.core.api.library;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;

/**
 * A File that is displayed in a library
 */
public interface FileItem {
    
    String getName();
    
    String getFileName(); 
    
    long getSize();

    long getCreationTime();

    long getLastModifiedTime();

    int getNumHits();

    int getNumUploads();

    Category getCategory();

    /**
     * Returns xml data about this fileItem
     */
    Object getProperty(FilePropertyKey key);

    URN getUrn();
    
}
