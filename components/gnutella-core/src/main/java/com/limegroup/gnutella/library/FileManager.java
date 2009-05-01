package com.limegroup.gnutella.library;

// TODO break extends of FileCollectionManager & FileViewManager -- it's just too much a PITA to fix right now
public interface FileManager extends FileCollectionManager {

    /** Asynchronously loads all files by calling loadSettings. */
    void start();

    void stop();
    
    /** Returns the {@link Library}. */
    Library getLibrary();
}