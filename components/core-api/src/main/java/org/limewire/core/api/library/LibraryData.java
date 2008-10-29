package org.limewire.core.api.library;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Allows manipulation of the library.
 */
public interface LibraryData {
    
    /**
     * Adds a directory that will not be scanned for managed files.
     * This will have no effect if a parent of this folder is not already
     * in the list of recursively managed directories.
     */
    void addDirectoryToExcludeFromManaging(File folder);
    
    /**
     * Adds a directory to scan recursively for managed files.
     * The only files included in management will be those that have
     * extensions matching the managed extensions.
     */
    void addDirectoryToManageRecursively(File folder);
    
    /**
     * Returns a list of all directories that will be managed recursively.
     */
    List<File> getDirectoriesToManageRecursively();

    /**
     * Returns a collection of all extensions that define which
     * files will be managed when folders are managed.
     */
    Collection<String> getManagedExtensions();

    /**
     * Returns a list of all extensions that this, by default, would
     * manage.  This is not necessarily the current managed list.
     */
    Collection<String> getDefaultManagedExtensions();

    /**
     * Sets the current managed extensions to be the given set.
     */
    void setManagedExtensions(Collection<String> extensions);
}
