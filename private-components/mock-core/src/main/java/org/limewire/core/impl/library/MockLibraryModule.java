package org.limewire.core.impl.library;

import org.limewire.core.api.library.LibraryManager;

import com.google.inject.AbstractModule;


public class MockLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(MockLibraryManager.class);
    }

}
