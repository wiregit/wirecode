package org.limewire.net.address;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NetworkManagerEvent;

public class AddressEvent extends NetworkManagerEvent {
    private final Address address;

    public AddressEvent(NetworkManager source, NetworkManager.EventType event, Address address) {
        super(source, event);
        this.address = address;
    }
    
    public Address getAddress() {
        return address;
    }
}
