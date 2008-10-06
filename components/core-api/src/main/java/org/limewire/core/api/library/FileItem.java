package org.limewire.core.api.library;

import org.limewire.core.api.Category;

/**
 * A File that is displayed in a library
 */
public interface FileItem {
    
    public static enum Keys {
        IMAGE, TITLE, AUTHOR, ALBUM, LENGTH, GENRE, BITRATE, TRACK, SAMPLE_RATE, YEAR, RATING, COMMENTS, HEIGHT, MISCELLANEOUS
    }
    
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
    Object getProperty(Keys key);

    void setProperty(Keys key, Object object);
    
}
