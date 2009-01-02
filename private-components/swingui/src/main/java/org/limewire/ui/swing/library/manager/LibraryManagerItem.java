package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Wraps a file in a directory and a boolean value for whether this
 * directory should be scanned.
 */
public interface LibraryManagerItem {
    
    /** Returns the File this is an item for. */
    public File getFile();
    
    /** Returns all children of this item. */
    public List<LibraryManagerItem> getChildren();
    
    /** Returns the render name of this item. */
    public String displayName();
    
    /** Returns the item that is this' parent. */
    public LibraryManagerItem getParent();

    /** Returns all Files that are subfolders but not listed as children. */
    public Collection<? extends File> getExcludedChildren();

    /** Adds the given child to the list of items to manage. */
    public int addChild(LibraryManagerItem child);

    /** Removes the given child from the list of items to manage. */
    public int removeChild(LibraryManagerItem item);

    /** Returns the LibraryManagerItem for a particular child directory. */
    public LibraryManagerItem getChildFor(File directory);
}
