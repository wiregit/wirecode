package com.limegroup.gnutella.library;

/** A manager for retrieving named file views. */
public interface FileViewManager {
    
    /**
     * Returns a {@link FileView} that contains all files visible to the given
     * id.
     */
    public FileView getFileViewForId(String id);

}
