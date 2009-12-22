package org.limewire.promotion.search;

import org.limewire.core.api.search.store.StoreAuthState;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.inject.AbstractModule;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.CachingEventMulticaster;
import org.limewire.listener.CachingEventMulticasterImpl;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.ListenerSupport;

import com.google.inject.TypeLiteral;

public class LimeWirePromotionSearchModule extends AbstractModule {
    @Override
    protected void configure() {
        
        CachingEventMulticaster<StoreAuthState> storeAuthStateBroadcaster =
            new CachingEventMulticasterImpl<StoreAuthState>(BroadcastPolicy.IF_NOT_EQUALS);
        storeAuthStateBroadcaster.broadcast(new StoreAuthState(false));
        bind(new TypeLiteral<EventBean<StoreAuthState>>(){}).toInstance(storeAuthStateBroadcaster);
        bind(new TypeLiteral<CachingEventMulticaster<StoreAuthState>>(){}).toInstance(storeAuthStateBroadcaster);
        bind(new TypeLiteral<ListenerSupport<StoreAuthState>>(){}).toInstance(storeAuthStateBroadcaster);
        bind(new TypeLiteral<EventBroadcaster<StoreAuthState>>(){}).toInstance(storeAuthStateBroadcaster);
        
        bind(StoreManager.class).to(CoreStoreManager.class);
        bind(StoreConnection.class).to(CoreStoreConnection.class);
        //bind(StoreConnectionFactory.class).toProvider(FactoryProvider.newFactory(
        //        StoreConnectionFactory.class, CoreStoreConnection.class));
    }
}
