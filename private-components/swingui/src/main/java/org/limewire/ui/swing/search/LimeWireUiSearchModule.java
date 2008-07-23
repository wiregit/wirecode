package org.limewire.ui.swing.search;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;


public class LimeWireUiSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchHandler.class).to(SearchHandlerImpl.class);
        bind(SearchResultsPanelFactory.class).toProvider(FactoryProvider.newFactory(SearchResultsPanelFactory.class, SearchResultsPanel.class));
    }

}
