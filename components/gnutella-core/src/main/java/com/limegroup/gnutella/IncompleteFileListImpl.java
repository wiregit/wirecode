package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

/**
 * A collection of IncompleteFileDescs.
 */
public class IncompleteFileListImpl extends FileListImpl {

    public IncompleteFileListImpl(FileManager fileManager, Set<File> individualFiles) {
        super(fileManager, individualFiles);
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
