package org.limewire.ui.swing.library.manager;

import java.util.List;

/**
 * Wraps a file in a directory and a boolean value for whether this
 * directory should be scanned.
 */
public interface LibraryManagerItem {

    public void setScanned(boolean value);
    
    public boolean isScanned();
    
    public List<LibraryManagerItem> getChildren();
    
    public String displayName();
}
