package com.limegroup.gnutella.dht.messages;

import java.util.Collection;
import java.util.Map.Entry;

import org.limewire.mojito.KUID;
import org.limewire.mojito.messages.StoreResponse;


public class StoreResponseWireImpl extends AbstractMessageWire<StoreResponse> 
        implements StoreResponse {

    StoreResponseWireImpl(StoreResponse delegate) {
        super(delegate);
    }

    public Collection<? extends Entry<KUID, Status>> getStatus() {
        return delegate.getStatus();
    }
}
