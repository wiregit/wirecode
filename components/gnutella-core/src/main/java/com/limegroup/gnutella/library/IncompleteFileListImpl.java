package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Executor;


/**
 * A collection of IncompleteFileDescs.
 */
class IncompleteFileListImpl extends FileListImpl {

    public IncompleteFileListImpl(Executor executor, FileManagerImpl fileManager, Set<File> individualFiles) {
        super(executor, fileManager, individualFiles);
    }
    
    @Override
    public void add(File file) {
        throw new UnsupportedOperationException("cannot add from here");
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
