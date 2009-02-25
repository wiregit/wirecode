package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchFactory;

import com.google.inject.AbstractModule;

public class MockSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchFactory.class).to(MockSearchFactory.class);
    }

}
