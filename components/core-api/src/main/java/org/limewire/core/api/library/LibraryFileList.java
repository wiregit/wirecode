package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;
import java.io.File;

import org.limewire.core.api.URN;

public interface LibraryFileList extends LocalFileList {

    /** Returns true if the library contains this file. */
    public boolean contains(File file);

    /** Returns true if the library contains a file with this URN. */
    public boolean contains(URN urn);
    
    /** Returns the current state of the library. */
    LibraryState getState();
 
    
    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
