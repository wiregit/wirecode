package com.limegroup.gnutella.library;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileManagerStub implements FileManager {

    private ManagedFileListStub managedStub;
    private IncompleteFileListStub incompleteStub;
    private GnutellaCollectionStub gnutellaStub;    
    private GnutellaViewStub gnutellaViewStub;
    
    @Inject
    public FileManagerStub() {    
        gnutellaStub = new GnutellaCollectionStub();
        incompleteStub = new IncompleteFileListStub();
        managedStub = new ManagedFileListStub();
    }
    
    @Override
    public GnutellaCollectionStub getGnutellaCollection() {
        return gnutellaStub;
    }
    
    @Override
    public IncompleteFileListStub getIncompleteFileCollection() {
        return incompleteStub;
    }
    
    @Override
    public ManagedFileListStub getLibrary() {
        return managedStub;
    }   

    
    @Override
    public SharedFileCollection getSharedCollection(int collectionId) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public SharedFileCollection getOrCreateSharedCollection(int collectionId) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public void removeSharedCollection(int collectionId) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public FileView getFileViewForId(String friendId) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public GnutellaFileView getGnutellaFileView() {
        return gnutellaViewStub;
    }
    
    @Override
    public void start() {}
    
    @Override
    public void stop() {}

    public void unloadFilesForFriend(String name) { }


}

