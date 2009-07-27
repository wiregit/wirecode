package org.limewire.core.api.search;

import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;

/**
 * Defines a Lime Store result.
 */
public interface StoreResult {

    /**
     * Returns the category for the result.
     */
    Category getCategory();
    
    /**
     * Returns true if the result represents a collection of audio files.
     */
    boolean isCollection();
    
    /**
     * Returns the file extension for the result.
     */
    String getFileExtension();
    
    /**
     * Returns a List of media files associated with the result.
     */
    List<SearchResult> getFileList();
    
    /**
     * Returns a property value for the specified property key.
     */
    Object getProperty(FilePropertyKey key);
    
    /**
     * Returns the total file size in bytes.
     */
    long getSize();
    
    /**
     * Returns the URN for the result.
     */
    URN getUrn();
}
