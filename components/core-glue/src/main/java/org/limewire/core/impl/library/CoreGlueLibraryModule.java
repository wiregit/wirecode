package org.limewire.core.impl.library;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.inject.AbstractModule;

public class CoreGlueLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(LibraryManagerImpl.class);
    }

}
