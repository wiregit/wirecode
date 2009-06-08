package org.limewire.ui.swing.search.model.browse;


import com.google.inject.AbstractModule;

public class LimeWireUiBrowseModule extends AbstractModule {
    
    @Override
    protected void configure() {
        
        bind(BrowseSearchFactory.class);
    }
}
