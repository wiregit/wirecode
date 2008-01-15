package com.limegroup.gnutella.downloader.serial;

import java.util.List;

/**
 * Allows all downloads to be written & read from disk.
 */
public interface DownloadSerializer {
    
    /** Reads all saved downloads from disk. */
    public List<DownloadMemento> readFromDisk();
    
    /** Writes all mementos to disk. */
    public boolean writeToDisk(List<? extends DownloadMemento> mementos);    
}
