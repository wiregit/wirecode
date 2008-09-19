package org.limewire.net.address;

import org.limewire.io.Address;

/**
 * Defines the requirements for an entity that can resolve addresses to other addresses.
 */
public interface AddressResolver {

    /**
     * Returns true if it can resolve the given type of address.
     */
    boolean canResolve(Address address);
    
    /**
     * Asynchronously resolves the address to possibly several other addresses and
     * notifies <code>observer</code> of the resolved addresses.
     */
    void resolve(Address address, int timeout, AddressResolutionObserver observer);
    
}
