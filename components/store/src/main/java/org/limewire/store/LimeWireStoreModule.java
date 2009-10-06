package org.limewire.store;

import org.limewire.inject.AbstractModule;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreConnectionFactory;

import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireStoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StoreManager.class).to(CoreStoreManager.class);
        bind(StoreConnectionFactory.class).toProvider(FactoryProvider.newFactory(
                StoreConnectionFactory.class, CoreStoreConnection.class));
    }
}
