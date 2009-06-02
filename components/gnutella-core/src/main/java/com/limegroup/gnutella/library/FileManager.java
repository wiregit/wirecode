package com.limegroup.gnutella.library;

public interface FileManager {

    /** Asynchronously loads all files by calling loadSettings. */
    void start();

    void stop();
    
    /** Returns the {@link Library}. */
    Library getLibrary();
}