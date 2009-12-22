package org.limewire.core.api.search.store;

import java.util.List;

import javax.swing.Icon;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;

/**
 * Defines a Lime Store result.
 */
public interface ReleaseResult {
    /** Sort priority values. */
    public enum SortPriority {
        TOP, MIXED, BOTTOM
    }
    
    public enum Type {
        TRACK, ALBUM          
    }

    /**
     * Adds the specified listener to the list that is notified when the 
     * store result is updated.
     */
    void addStoreResultListener(StoreResultListener listener);
    
    /**
     * Removes the specified listener from the list that is notified when the
     * store result is updated.
     */
    void removeStoreResultListener(StoreResultListener listener);
    
    Type getType();
    
    /**
     * Returns true if the result represents a collection of media files.
     */
    boolean isAlbum();
    
    /**
     * Returns the icon associated with the album.
     */
    Icon getAlbumIcon();
    
    /**
     * Returns the album identifier.
     */
    String getAlbumId();
    
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
     * Returns the URI of the stream for the result.
     */
    String getStreamURI();
    
    /**
     * Returns the number of tracks in an album.
     */
    long getTrackCount();
    
    /**
     * Returns a List of media files associated with the result.
     */
    List<TrackResult> getTracks();
    
    /**
     * Returns the URN for the result.
     */
    URN getUrn();
}
