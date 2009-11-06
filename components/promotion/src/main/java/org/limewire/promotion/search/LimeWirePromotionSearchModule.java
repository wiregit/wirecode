package org.limewire.promotion.search;

import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.inject.AbstractModule;

public class LimeWirePromotionSearchModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StoreManager.class).to(CoreStoreManager.class);
        bind(StoreConnection.class).to(CoreStoreConnection.class);
        //bind(StoreConnectionFactory.class).toProvider(FactoryProvider.newFactory(
        //        StoreConnectionFactory.class, CoreStoreConnection.class));
    }
}
