package org.limewire.core.api.playlist;

/**
 * Handles reading and writing of M3U lists into and out of
 * LocalFileLists. 
 */
public interface M3UList {

    /**
     * Loads the given M3U list.
     */
    public void load();
    
    /**
     * Saves the given M3U list.
     */
    public void save();
}
