package org.limewire.net.address;

import org.limewire.io.Address;
import org.limewire.nio.observer.IOErrorObserver;

/**
 * Defines the callback that can be notified of a done address resolution.
 */
public interface AddressResolutionObserver extends IOErrorObserver {

    /**
     * Called with the resolved addresses. 
     */
    void resolved(Address...addresses);
}
