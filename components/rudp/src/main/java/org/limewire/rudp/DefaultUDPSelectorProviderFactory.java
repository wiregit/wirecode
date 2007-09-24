package org.limewire.rudp;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Returns a single provider created with the given context. */
@Singleton
public class DefaultUDPSelectorProviderFactory implements UDPSelectorProviderFactory, Provider<UDPSelectorProvider> {

    private final UDPSelectorProvider provider;

    @Inject
    public DefaultUDPSelectorProviderFactory(RUDPContext context) {
        this.provider = new UDPSelectorProvider(context);
    }

    /** Returns a singleton of the provider. */
    public UDPSelectorProvider createProvider() {
        return provider;
    }
    
    public UDPSelectorProvider get() {
        return provider;
    }

}
