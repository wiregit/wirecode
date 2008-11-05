package org.limewire.core.api.library;

import java.io.File;

import org.limewire.core.api.URN;

/** A list of FileItems that are locally stored on disk. */
public interface LocalFileList extends FileList<LocalFileItem> {

    /** Adds the given file to the list. */
    public void addFile(File file);
    
    /** Removes the given file from the list. */
    public void removeFile(File file);
    
    /** Adds all files in the folder to the list. */
    public void addFolder(File folder);
    
    /** Returns true if the list contains this file. */
    public boolean contains(File file);

    /** Returns true if the list contains a file with this URN. */
    public boolean contains(URN urn);

}
