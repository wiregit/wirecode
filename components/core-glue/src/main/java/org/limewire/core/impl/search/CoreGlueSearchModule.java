package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.actions.FromActions;
import org.limewire.core.impl.search.actions.FromActionsImpl;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchFactory.class).toProvider(FactoryProvider.newFactory(SearchFactory.class, CoreSearch.class));
        bind(FromActions.class).to(FromActionsImpl.class);
    }

}
