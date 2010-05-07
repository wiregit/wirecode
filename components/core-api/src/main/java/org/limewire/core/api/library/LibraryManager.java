package org.limewire.core.api.library;

/** Exposes information about the library. */
public interface LibraryManager {
    
    /** Returns the list from which all local library data is stored. */
    LibraryFileList getLibraryManagedList();
    
    /** Returns an object from which more data about the library can be queried. */
    LibraryData getLibraryData();
}
