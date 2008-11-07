package org.limewire.core.api.library;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Allows manipulation of the library.
 */
public interface LibraryData {
    
    /**
     * Sets the new directories to recursively manage, and directories
     * to be excluded from management.
     */
    void setManagedFolders(Collection<File> recursiveFoldersToManage, Collection<File> foldersToExclude);
    
    /**
     * Returns a list of all directories that will be managed recursively.
     */
    List<File> getDirectoriesToManageRecursively();
    
    /** Returns a list of all excluded directories. */
    List<File> getDirectoriesToExcludeFromManaging();

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

    /** Returns true if the directory is allowed to be recursively managed. */
    boolean isDirectoryAllowed(File folder);

    /** Returns true if the directory is allowed to be recursively managed. */
    boolean isDirectoryExcluded(File folder);
}
