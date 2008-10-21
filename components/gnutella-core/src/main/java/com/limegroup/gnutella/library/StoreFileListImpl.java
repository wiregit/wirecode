package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.limewire.core.settings.SharingSettings;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.xml.LimeXMLDocument;



/**
 * A collection of FileDescs containing only files purchased from the LWS.
 */
class StoreFileListImpl extends FileListImpl {

    public StoreFileListImpl(Executor executor, FileManagerImpl fileManager, Set<File> individualFiles) {
        super(executor, fileManager, individualFiles);
    }
    
    @Override
    public void add(File file) {
        throw new UnsupportedOperationException("cannot add");
    }
    
    @Override
    public void add(File file, List<LimeXMLDocument> documents) {
        throw new UnsupportedOperationException("cannot add");
    }
    
    @Override
    public void addForSession(File file) {
        throw new UnsupportedOperationException("will not add");
    }
    
    /**
     * Store files shouldn't be constrained by waiting for pending files. It may be
     * possible the file was in another directory and it wasn't till the XML was read
     * that it was discovered to be a store file
     */
    @Override
    protected void addPendingFileDesc(FileDesc fileDesc) {
        add(fileDesc);
    }
    
    /**
     * If the store directory is not in its path, its not a store file
     */
    @Override
    protected void addAsIndividualFile(FileDesc fileDesc) {
        // if this file isn't in the store folder, add to individual store files 
        if(!FileUtils.isAncestor(SharingSettings.getSaveLWSDirectory(), fileDesc.getFile())) { 
            individualFiles.add(fileDesc.getFile());
        } 
    }
   
    /**
     * Only allow FileDescs from store
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        // if this file doesn't contain store encoding, don't add this FileDesc
        if( fileDesc.getLimeXMLDocuments().size() == 0 || 
                !isStoreXML(fileDesc.getLimeXMLDocuments().get(0))) {
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    protected void fireAddEvent(FileDesc fileDesc) {
        fileDesc.setStoreFile(true);
        super.fireAddEvent(fileDesc);
    }

    @Override
    protected void fireRemoveEvent(FileDesc fileDesc) {
        fileDesc.setStoreFile(false);
        super.fireRemoveEvent(fileDesc);
    }

    @Override
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        oldFileDesc.setStoreFile(false);
        newFileDesc.setStoreFile(true);
        super.fireChangeEvent(oldFileDesc, newFileDesc);
    }
}
