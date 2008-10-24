package org.limewire.http.auth;

import org.limewire.inject.AbstractModule;

public class LimeWireHttpAuthModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(RequestAuthenticator.class).to(RequestAuthenticatorImpl.class);
        bind(AuthenticatorRegistry.class).to(AuthenticatorRegistryImpl.class);
        bind(Authenticator.class).to(AuthenticatorRegistryImpl.class);
    }
}
