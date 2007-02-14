package com.limegroup.gnutella.rudp;

import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.rudp.UDPSelectorProviderFactory;

/** Returns the wire-enabled singleton provider. */
public class UDPSelectorProviderFactoryWire implements UDPSelectorProviderFactory {

    private static final UDPSelectorProvider provider = new UDPSelectorProvider(new RUDPContextWire());

    /** Returns a singleton of the provider. */
    public UDPSelectorProvider createProvider() {
        return provider;
    }

}
