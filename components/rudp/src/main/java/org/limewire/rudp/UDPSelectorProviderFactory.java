package org.limewire.rudp;

/** Defines an interface that creates selector providers. */
public interface UDPSelectorProviderFactory {

    /** Returns a new selector provider. */
    public UDPSelectorProvider createProvider();
    
}
