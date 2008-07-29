package com.limegroup.gnutella;

import java.io.File;

/**
 * Extra methods for FileManager to access but not part of the public API.
 */
public interface FileListPackage extends FileList {

    /**
     * Adds the file to the pending file list. Once this
     * FileDesc has been calculated, this list will automatically
     * add the FileDesc to itself. This FileDesc is added despite
     * it not being addable to the list.
     */
    public void addPendingFileAlways(File file);
    
    /**
     * Adds the file to the pending file list. Once this
     * FileDesc has been calculated, this list will automatically
     * add the FileDesc to itself. This FileDesc is only added to
     * this list for the current session.
     */
    public void addPendingFileForSession(File file);
    
    /**
     * Adds the file to the pending file list. Once this
     * FileDesc has been calculated, this list will automatically
     * add the FileDesc to itself.
     */
    public void addPendingFile(File file);
}
