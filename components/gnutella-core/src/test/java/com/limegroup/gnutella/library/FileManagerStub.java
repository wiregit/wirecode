package com.limegroup.gnutella.library;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileManagerStub implements FileManager, FileViewManager {

    private LibraryStub managedStub;
    private IncompleteFileCollectionStub incompleteStub;
    private GnutellaFileCollectionStub gnutellaStub; 
    
    @Inject
    public FileManagerStub() {    
        gnutellaStub = new GnutellaFileCollectionStub();
        incompleteStub = new IncompleteFileCollectionStub();
        managedStub = new LibraryStub();
    }
    
    @Override
    public GnutellaFileCollectionStub getGnutellaCollection() {
        return gnutellaStub;
    }
    
    @Override
    public IncompleteFileCollectionStub getIncompleteFileCollection() {
        return incompleteStub;
    }
    
    @Override
    public LibraryStub getLibrary() {
        return managedStub;
    }   
    
    @Override
    public SharedFileCollection createNewCollection(String name) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public SharedFileCollection getOrCreateCollectionByName(String name) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public void removeCollectionByName(String name) {
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
    public FileView getCompositeSharedView() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileView getFileViewForId(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GnutellaFileView getGnutellaFileView() {
        return gnutellaStub;
    }

    @Override
    public FileView getIncompleteFileView() {
        return incompleteStub;
    }


}

