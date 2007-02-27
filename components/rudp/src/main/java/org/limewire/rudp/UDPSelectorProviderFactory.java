package org.limewire.rudp;

/** A factory for creating selector providers. */
public interface UDPSelectorProviderFactory {

    /** Returns a new selector provider. */
    public UDPSelectorProvider createProvider();
    
}
