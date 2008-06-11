package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManagerController;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.FileManagerImpl;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.FileManagerEvent.Type;
import com.limegroup.gnutella.downloader.VerifyingFile;

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
    public FileDesc getFileDescForUrn(final URN urn) {
        return sharedFileList.getFileDesc(urn);
    }
    
    @Override
    public void fileChanged(File f) {
        throw new UnsupportedOperationException();
    }
    
    public List getRemoveRequests() {
        return removeRequests;
    }
    
    @Override
    public synchronized void addIncompleteFile(File incompleteFile,
            Set<? extends URN> urns,
            String name,
            long size,
            VerifyingFile vf) {
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
        } catch(IOException ioe) {
        //invalid file?... don't add incomplete file.
            return;
        }

        // no indices were found for any URN associated with this
        // IncompleteFileDesc... add it.
        int fileIndex = sharedFileList.getListLength();
        
        IncompleteFileDesc ifd = new IncompleteFileDesc(
        incompleteFile, urns, fileIndex, name, size, vf);
        sharedFileList.addIncompleteFile(incompleteFile, ifd);
        fileURNSUpdated(ifd);
        
        _needRebuild = true;
        dispatchFileEvent(new FileManagerEvent(this, Type.ADD_FILE, ifd));
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

