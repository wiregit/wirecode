package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import com.limegroup.gnutella.xml.LimeXMLDocument;


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
    public void add(File file, List<LimeXMLDocument> documents) {
        throw new UnsupportedOperationException("cannot add from here");
    }
    
    @Override
    public void addForSession(File file) {
        throw new UnsupportedOperationException("will not add");
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
