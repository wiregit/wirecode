package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.actions.FromActions;
import org.limewire.core.impl.search.actions.FromActionsMockImpl;

import com.google.inject.AbstractModule;

public class MockSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchFactory.class).to(MockSearchFactory.class);
        bind(FromActions.class).to(FromActionsMockImpl.class);
    }

}
