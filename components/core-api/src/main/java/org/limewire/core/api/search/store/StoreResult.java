package org.limewire.core.api.search.store;

import java.util.List;

import javax.swing.Icon;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;

/**
 * Defines a Lime Store result.
 */
public interface StoreResult {
    /** Sort priority values. */
    public enum SortPriority {
        TOP, MIXED, BOTTOM;
    }

    /**
     * Returns true if the result represents a collection of media files.
     */
    boolean isAlbum();
    
    /**
     * Returns the icon associated with the album.
     */
    Icon getAlbumIcon();
    
    /**
     * Returns a List of media files associated with the result.
     */
    List<TrackResult> getAlbumResults();
    
    /**
     * Returns the category for the result.
     */
    Category getCategory();
    
    /**
     * Returns the file extension for the result.
     */
    String getFileExtension();
    
    /**
     * Returns the file name for the result.
     */
    String getFileName();
    
    /**
     * Returns the URI of the info page for the result.
     */
    String getInfoURI();
    
    /**
     * Returns the price for the result.
     */
    String getPrice();
    
    /**
     * Returns a property value for the specified property key.
     */
    Object getProperty(FilePropertyKey key);
    
    /**
     * Returns the total file size in bytes.
     */
    long getSize();
    
    /**
     * Returns the sort priority for the result.
     */
    SortPriority getSortPriority();
    
    /**
     * Returns the source for the result as a RemoteHost object.
     */
    RemoteHost getSource();
    
    /**
     * Returns the URN for the result.
     */
    URN getUrn();
}
