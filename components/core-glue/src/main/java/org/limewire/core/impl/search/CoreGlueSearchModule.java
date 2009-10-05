package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchEvent;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.store.StoreConnectionFactory;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.impl.search.store.CoreStoreConnection;
import org.limewire.core.impl.search.store.CoreStoreManager;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * Guice module to configure the Search API for the live core. 
 */
public class CoreGlueSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchFactory.class).toProvider(FactoryProvider.newFactory(
                SearchFactory.class, CoreSearch.class));
        bind(StoreManager.class).to(CoreStoreManager.class);
        bind(StoreConnectionFactory.class).toProvider(FactoryProvider.newFactory(
                StoreConnectionFactory.class, CoreStoreConnection.class));
        
        EventMulticaster<SearchEvent> searchMulticaster = new EventMulticasterImpl<SearchEvent>();
        bind(new TypeLiteral<EventBroadcaster<SearchEvent>>(){}).toInstance(searchMulticaster);
        bind(new TypeLiteral<ListenerSupport<SearchEvent>>(){}).toInstance(searchMulticaster);
        
        bind(RemoteFileDescAdapter.Factory.class).toProvider(FactoryProvider.newFactory(RemoteFileDescAdapter.Factory.class, RemoteFileDescAdapter.class));
    }

}
