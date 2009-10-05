package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchEvent;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.store.StoreConnectionFactory;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.impl.search.store.MockStoreConnection;
import org.limewire.core.impl.search.store.MockStoreManager;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class MockSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchFactory.class).to(MockSearchFactory.class);
        bind(StoreManager.class).to(MockStoreManager.class);
        
        bind(StoreConnectionFactory.class).toProvider(
                FactoryProvider.newFactory(
                        StoreConnectionFactory.class, MockStoreConnection.class));
        
        EventMulticaster<SearchEvent> searchMulticaster = new EventMulticasterImpl<SearchEvent>();
        bind(new TypeLiteral<EventBroadcaster<SearchEvent>>(){}).toInstance(searchMulticaster);
        bind(new TypeLiteral<ListenerSupport<SearchEvent>>(){}).toInstance(searchMulticaster);
    }

}
