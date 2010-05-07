package org.limewire.core.api.library;

import java.io.File;

/**
 * Allows manipulation of the library.
 */
public interface LibraryData {        

    /** Returns true if this file is potentially manageable. */
    boolean isFileManageable(File f);
    
    /** Returns the number of files in a the share list directly from the raw library db */
    int peekPublicSharedListCount();
}
