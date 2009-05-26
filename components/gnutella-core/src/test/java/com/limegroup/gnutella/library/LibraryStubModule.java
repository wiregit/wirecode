package com.limegroup.gnutella.library;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class LibraryStubModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FileManager.class).to(FileManagerStub.class);
        bind(FileCollectionManager.class).to(FileManagerStub.class);
        bind(FileViewManager.class).to(FileManagerStub.class);
    }
    
    @Provides Library library(FileManagerStub stub) {
        return stub.getLibrary();
    }
    
    @Provides GnutellaFileCollection gfc(FileManagerStub stub) {
        return stub.getGnutellaCollection();
    }
    
    @Provides GnutellaFileView gfv(FileManagerStub stub) {
        return stub.getGnutellaCollection();
    }
    
    @Provides IncompleteFileCollection ifc(FileManagerStub stub) {
        return stub.getIncompleteFileCollection();
    }
    
    @Provides @IncompleteView FileView ifv(FileManagerStub stub) {
        return stub.getIncompleteFileCollection();
    }

}
