package org.limewire.core.api.library;

/**
 * A File that is displayed in a library
 */
public interface FileItem {
    
    //TODO: there's about three identical categories floating around
    public static enum Category {
        VIDEO, AUDIO, DOCUMENT, IMAGE, PROGRAM, OTHER
    }

    public static enum Keys {
        IMAGE, TITLE, AUTHOR, ALBUM
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
