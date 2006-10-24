package com.limegroup.gnutella.dht.messages;

import java.util.Collection;
import java.util.Map.Entry;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.StoreResponse;

public class StoreResponseWireImpl extends AbstractMessageWire<StoreResponse> 
        implements StoreResponse {

    StoreResponseWireImpl(StoreResponse delegate) {
        super(delegate);
    }

    public Collection<Entry<KUID, Status>> getStatus() {
        return delegate.getStatus();
    }
}
