package com.limegroup.gnutella.library;

import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileManagerStub implements FileManager, FileCollectionManager, ListenerSupport<FileViewChangeEvent> {

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
    public void start() {}
    
    @Override
    public void stop() {}

    FileView getGnutellaFileView() {
        return gnutellaStub;
    }

    FileView getIncompleteFileView() {
        return incompleteStub;
    }

    @Override
    public SharedFileCollection getSharedFileCollection() {
        return gnutellaStub;
    }

    @Override
    public void addListener(EventListener<FileViewChangeEvent> listener) {
    }
    
    @Override
    public boolean removeListener(EventListener<FileViewChangeEvent> listener) {
        return false;
    }    
}