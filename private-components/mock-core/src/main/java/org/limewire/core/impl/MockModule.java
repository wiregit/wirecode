package org.limewire.core.impl;

import org.limewire.core.api.Application;
import org.limewire.core.impl.download.MockDownloadModule;
import org.limewire.core.impl.library.MockLibraryModule;
import org.limewire.core.impl.search.MockSearchModule;

import com.google.inject.AbstractModule;


public class MockModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(Application.class).to(MockApplication.class);
        
        install(new MockSearchModule());
        install(new MockDownloadModule());
        install(new MockLibraryModule());
        
    }

}
