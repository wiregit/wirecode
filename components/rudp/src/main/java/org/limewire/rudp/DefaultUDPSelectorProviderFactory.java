package org.limewire.rudp;

/** Returns a single provider created with the given context. */
public class DefaultUDPSelectorProviderFactory implements UDPSelectorProviderFactory {

    private final UDPSelectorProvider provider;
    
    public DefaultUDPSelectorProviderFactory(RUDPContext context) {
        this.provider = new UDPSelectorProvider(context);
    }

    /** Returns a singleton of the provider. */
    public UDPSelectorProvider createProvider() {
        return provider;
    }

}
