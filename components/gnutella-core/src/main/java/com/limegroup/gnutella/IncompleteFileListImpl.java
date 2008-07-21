package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

/**
 * FileList that contains only IncompleteFileDescs
 */
public class IncompleteFileListImpl extends FileListImpl {

    public IncompleteFileListImpl(String name, FileManager fileManager, Set<File> individualFiles) {
        super(name, fileManager, individualFiles);
    }
    
    @Override
    protected void addPendingFileDesc(FileDesc fileDesc) { 
        if(fileDesc instanceof IncompleteFileDesc) {
            addFileDesc(fileDesc);
        }
    }
    
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        return fileDesc instanceof IncompleteFileDesc;
    }
}
