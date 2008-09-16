package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.nio.observer.ConnectObserver;

/**
 * Defines the requirements for an entity that can connect to an address
 * and create a socket.
 */
public interface AddressConnector {

    /**
     * Returns true if it can connect to the given type of address.
     */
    boolean canConnect(Address address);

    /**
     * Connects asynchronously to the given address and notifies 
     * <code>observer</code> of the established socket.
     */
    void connect(Address address, int timeout, ConnectObserver observer);
    
}
