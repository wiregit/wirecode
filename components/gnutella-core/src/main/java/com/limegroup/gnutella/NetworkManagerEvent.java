package com.limegroup.gnutella;

import org.limewire.listener.DefaultEvent;

import com.limegroup.gnutella.NetworkManager.EventType;

public class NetworkManagerEvent extends DefaultEvent<NetworkManager, EventType> {

    public NetworkManagerEvent(NetworkManager source, EventType event) {
        super(source, event);
    }
}
