package com.limegroup.gnutella.stubs;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManagerController;
import com.limegroup.gnutella.FileManagerImpl;

/**
 * A simple FileManager that shares one file of (near) infinite length.
 */
@SuppressWarnings("unchecked")
@Singleton
public class FileManagerStub extends FileManagerImpl {

    
    @Inject
    public FileManagerStub(FileManagerController fileManagerController) {
        super(fileManagerController);
        
        sharedFileList = new FileListStub();
    }
    
    private List removeRequests = new LinkedList();

    
    
    @Override
    public void fileChanged(File f) {
        throw new UnsupportedOperationException();
    }
    
    public List getRemoveRequests() {
        return removeRequests;
    }
    
    @Override
    public synchronized FileDesc removeFileIfSharedOrStore(File f) {
        removeRequests.add(f);
        return super.removeFileIfSharedOrStore(f);
    }
    
    @Override
    protected synchronized FileDesc removeFileIfSharedOrStore(File f, boolean notify) {
        removeRequests.add(f);
        return super.removeFileIfSharedOrStore(f, notify);
    }

    @Override
    protected synchronized FileDesc removeFileIfShared(File f, boolean notify) {
        removeRequests.add(f);
        return super.removeFileIfShared(f, notify);
    }
}

