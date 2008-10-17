package org.limewire.http.auth;

import org.limewire.inject.AbstractModule;

public class LimeWireHttpAuthModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(RequestAuthenticator.class).to(RequestAuthenticatorImpl.class);
        bind(UserStoreRegistry.class).to(UserStoreRegistryImpl.class);
        bind(UserStore.class).to(UserStoreRegistryImpl.class);
    }
}
