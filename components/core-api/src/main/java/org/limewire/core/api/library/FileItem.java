package org.limewire.core.api.library;

import java.io.File;

/**
 * A File that is displayed in a library
 */
public interface FileItem {

    //TODO: there's about three identical categories floating around
    public static enum Category {
        VIDEO, AUDIO, DOCUMENT, IMAGE, PROGRAM, OTHER
    };
    
    File getFile();
    
    String getName();
    
    long getSize();
    
    long getCreationTime();
    
    long getLastModifiedTime();
    
    int getNumHits();
    
    int getNumUploads();
    
    Category getCategory();
    
    /**
     * Returns xml data about this fileItem
     */
    String getProperty(String key);
    
}
