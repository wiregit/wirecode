package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchEvent;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchManager;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchManager.class).to(CoreSearchManager.class);
        bind(SearchFactory.class).toProvider(FactoryProvider.newFactory(SearchFactory.class, CoreSearch.class));
        EventMulticaster<SearchEvent> searchMulticaster = new EventMulticasterImpl<SearchEvent>();
        bind(new TypeLiteral<EventBroadcaster<SearchEvent>>(){}).toInstance(searchMulticaster);
        bind(new TypeLiteral<ListenerSupport<SearchEvent>>(){}).toInstance(searchMulticaster);
        
        bind(RemoteFileDescAdapter.Factory.class).toProvider(FactoryProvider.newFactory(RemoteFileDescAdapter.Factory.class, RemoteFileDescAdapter.class));
        bind(TorrentWebSearchFactory.class).toProvider(FactoryProvider.newFactory(TorrentWebSearchFactory.class, TorrentWebSearch.class));
        bind(TorrentUriPrioritizerFactory.class).toProvider(FactoryProvider.newFactory(TorrentUriPrioritizerFactory.class, TorrentUriPrioritizerImpl.class));
        
        bind(TorrentUriStore.class).to(SqlTorrentUriStore.class);
    }

}
