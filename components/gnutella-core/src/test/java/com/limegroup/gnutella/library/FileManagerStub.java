package com.limegroup.gnutella.library;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileManagerStub implements FileManager, FileCollectionManager, FileViewManager {

    private LibraryStub managedStub;
    private IncompleteFileCollectionStub incompleteStub;
    private GnutellaFileCollectionStub gnutellaStub; 
    
    @Inject
    public FileManagerStub() {    
        gnutellaStub = new GnutellaFileCollectionStub();
        incompleteStub = new IncompleteFileCollectionStub();
        managedStub = new LibraryStub();
    }
    
    GnutellaFileCollectionStub getGnutellaCollection() {
        return gnutellaStub;
    }
    
    IncompleteFileCollectionStub getIncompleteFileCollection() {
        return incompleteStub;
    }
    
    LibraryStub getLibrary() {
        return managedStub;
    }   

    @Override
    public SharedFileCollection createNewCollection(String name) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public SharedFileCollection getCollectionById(int collectionId) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public void removeCollectionById(int collectionId) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public void start() {}
    
    @Override
    public void stop() {}

    public void unloadCollectionByName(String name) { }


    @Override
    public FileView getFileViewForId(String id) {
        throw new UnsupportedOperationException();
    }

    FileView getGnutellaFileView() {
        return gnutellaStub;
    }

    FileView getIncompleteFileView() {
        return incompleteStub;
    }

    @Override
    public List<SharedFileCollection> getSharedFileCollections() {
        // TODO Auto-generated method stub
        return null;
    }


}

