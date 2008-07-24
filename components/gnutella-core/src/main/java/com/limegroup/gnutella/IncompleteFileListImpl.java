package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

/**
 * A collection of IncompleteFileDescs.
 */
public class IncompleteFileListImpl extends FileListImpl {

    public IncompleteFileListImpl(String name, FileManager fileManager, Set<File> individualFiles) {
        super(name, fileManager, individualFiles);
    }
    
    @Override
    protected void addPendingFileDesc(FileDesc fileDesc) { 
        if(fileDesc instanceof IncompleteFileDesc) {
            add(fileDesc);
        }
    }
    
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        return fileDesc instanceof IncompleteFileDesc;
    }
}
